//
// $Id: $


package com.threerings.msoy.server;

import java.util.Date;
import java.util.Queue;

import com.google.common.collect.Lists;

import com.samskivert.util.Invoker;
import com.samskivert.util.Invoker.Unit;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import static com.threerings.msoy.Log.log;

/**
 * A queue that puts tasks onto the invoker thread sequentially, one after the other, and lets
 * all the tasks in the queue know what their position is.
 */
@Singleton @EventThread
public class ResolutionQueue
{
    @EventThread
    public static interface Listener
    {
        /** As a {@link Task} moves up in the queue, it will be informed through this callback. */
        void progress (int position);

        /** Called to announce the completion of your {@link Task}. */
        void done (Task task);

        /** Called to report an exception was thrown executing code in your {@link Task}. */
        void failed (Task task, Exception e);
    }

    public static interface Task
    {
        /** Perform here any blocking (e.g. database) operations your task needs. */
        @BlockingThread public void resolve () throws Exception;

        /** Perform any tasks that you need to be on the dobj thread for. */
        @EventThread public void handle () throws Exception;
    }

    /**
     * Queue another task, possibly starting the loop up if it isn't running.
     */
    public void addTask (Task task, Listener listener)
    {
        _queue.add(new Entry(task, listener));
        if (!_running) {
            loop();
        }
    }

    /**
     * Let interested parties know how long our queue is right now.
     */
    public int getQueueSize ()
    {
        return _queue.size();
    }

    /**
     * The main entry point of the loop, if we get here there should be entries to process.
     * We will yield the thread at the end of this function, the invoker will pick it up to
     * call resolve() in the entry's task, then we return to the dobj thread to let the trask
     * do processing on that level and then finally call {@link #aftermath}.
     */
    @EventThread
    protected void loop ()
    {
        // pop the next entry off the queue
        final Entry next = _queue.poll();
        if (next == null) {
            // internal error
            log.warning("Did not expect to find queue empty here");
            _running = false;
            return;
        }
        // update our notion of the oldest index in the queue
        _headIx = next.ix;

        // note that we're running
        _running = true;
        // and ask the invoker to execute the task
        _invoker.postUnit(new TaskUnit(next));
    }

    /**
     * When the invoker unit finishes its work both on the invoker thread and the dobj one and
     * any error handling has been dealt with, we end up here to update all the remaining entries
     * in the queue on what's happening and to see if we need to continue looping.
     */
    @EventThread
    protected void aftermath ()
    {
        // go through all existing entries
        for (Entry entry : _queue) {
            if (entry.listener != null) {
                // let it know what position in the queue it has
                entry.listener.progress(entry.ix - _headIx);
            }
        }
        if (_queue.isEmpty()) {
            _running = false;
        } else {
            loop();
        }
    }

    /**
     * An invoker unit wrapping a {@link Task}. It's only purpose is to call the relevant task
     * functions, to catch and handle errors, and to keep the loop running at all cost.
     */
    protected class TaskUnit extends Unit
    {
        public TaskUnit (Entry entry)
        {
            _entry = entry;
        }

        @BlockingThread
        public boolean invoke () {
            // fulfill the task's persistent yearnings
            try {
                _entry.task.resolve();

            } catch (Exception e) {
                // if there's an error, remember for later
                _failure = e;
            }
            return true;
        }

        @EventThread
        public void handleResult () {
            // if the persistent bit went off without problems...
            if (_failure == null) {
                try {
                    // ... let the task do whatever it needs to do on the dobj thread.
                    _entry.task.handle();

                } catch (Exception e) {
                    // if there's an error here, note that
                    _failure = e;
                }
            }

            if (_entry.listener != null) {
                try {
                    // call either the completion or the error handler depending on how it went
                    if (_failure == null) {
                        _entry.listener.done(_entry.task);
                    } else {
                        _entry.listener.failed(_entry.task, _failure);
                    }

                } catch (Exception e) {
                    // if the handler itself threw an exception, just log and keep going
                    log.warning("Error in handler for task", "task", _entry.task, e);
                }
            }

            // call back to the second half of our loop system
            aftermath();
        }

        final protected Entry _entry;
        protected Exception _failure;
    }

    protected class Entry
    {
        Task task;
        Listener listener;
        Date timestamp;
        int ix;

        protected Entry (Task task, Listener listener)
        {
            this.task = task;
            this.listener = listener;
            this.timestamp = new Date();
            this.ix = _nextIx ++;
        }
    }

    /** The list of entries we have to work through. */
    protected Queue<Entry> _queue = Lists.newLinkedList();

    /** The next unique integer to assign to a new entry. */
    protected int _nextIx;

    /** The index of the entry at the head of the queue, or of the last entry if it's empty. */
    protected int _headIx;

    /** Whether or not we currently have a unit in the invoker pipeline. */
    protected boolean _running;

    /** The invoker to which we submit tasks. */
    @Inject @MainInvoker protected Invoker _invoker;
}