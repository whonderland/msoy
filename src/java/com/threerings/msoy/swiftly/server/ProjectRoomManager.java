//
// $Id$

package com.threerings.msoy.swiftly.server;

import java.io.IOException;

import com.samskivert.util.SerialExecutor;
import com.threerings.util.MessageBundle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.swiftly.client.ProjectRoomService;
import com.threerings.msoy.swiftly.data.DocumentElement;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomConfig;
import com.threerings.msoy.swiftly.data.ProjectRoomMarshaller;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;

/**
 * Manages a Swiftly project room.
 */
public class ProjectRoomManager extends PlaceManager
    implements ProjectRoomProvider
{
    // from interface ProjectRoomProvider
    public void addPathElement (ClientObject caller, PathElement element)
    {
        // TODO: check access!

        // for now just update the room object
        _roomObj.addPathElement(element);
    }

    // from interface ProjectRoomProvider
    public void updatePathElement (ClientObject caller, PathElement element)
    {
        // TODO: check access!
        _roomObj.updateElements(element);
    }

    // from interface ProjectRoomProvider
    public void deletePathElement (ClientObject caller, int elementId)
    {
        // TODO: check access!
        _roomObj.removeFromElements(elementId);
    }

    // from interface ProjectRoomManager
    public void buildProject (ClientObject caller)
    {
        // TODO: check access!

        // issue a request on the executor to build this project; any log output should be
        // collected, then published on _roomObj.console
        MsoyServer.swiftlyMan.executor.addTask(new BuildProjectTask());
    }

    // from interface ProjectRoomManager
    public void commitProject (ClientObject caller, String commitMsg,
                               ProjectRoomService.ConfirmListener listener)
        throws InvocationException
    {
        // TODO: check access!

        // TODO: run the commit on the executor and post the result to the listener on success or
        // failure
        throw new InvocationException(SwiftlyCodes.INTERNAL_ERROR);
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new ProjectRoomObject();
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // get a casted reference to our room object
        _roomObj = (ProjectRoomObject)_plobj;

        // TODO: load up the project information and populate the room object
        PathElement node;
        _roomObj.addPathElement(node = PathElement.createRoot("Fake Project"));
        _roomObj.addPathElement(node = PathElement.createDirectory("Directory 1", node.elementId));
        _roomObj.addPathElement(new PathElement(PathElement.Type.FILE, "File 1", node.elementId));
        _roomObj.addPathElement(new PathElement(PathElement.Type.FILE, "File 2", node.elementId));
        _roomObj.addPathElement(new PathElement(PathElement.Type.FILE, "File 3", node.elementId));
        _roomObj.addPathElement(node = PathElement.createDirectory("Directory 2", node.elementId));
        _roomObj.addPathElement(new PathElement(PathElement.Type.FILE, "File 4", node.elementId));
        _roomObj.addPathElement(new PathElement(PathElement.Type.FILE, "File 5", node.elementId));
        _roomObj.addPathElement(new DocumentElement("File 6", node.elementId, "Bob!"));

        // wire up our invocation service
        _roomObj.setService(
            (ProjectRoomMarshaller)MsoyServer.invmgr.registerDispatcher(
                new ProjectRoomDispatcher(this), false));
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        super.didShutdown();
        MsoyServer.swiftlyMan.projectDidShutdown(this);
    }

    /** Handles a request to build our project. */
    protected class BuildProjectTask implements SerialExecutor.ExecutorTask
    {
        public BuildProjectTask () {
            _projectId = ((ProjectRoomConfig)_config).projectId;
        }

        public boolean merge (SerialExecutor.ExecutorTask other) {
            // we don't want more than one pending build for a project
            if (other instanceof BuildProjectTask) {
                return _projectId == ((BuildProjectTask)other)._projectId;
            }
            return false;
        }

        public long getTimeout () {
            return 60 * 1000L; // 60 seconds is all you get kid
        }

        // this is called on the executor thread and can go hog wild with the blocking
        public void executeTask () {
            try {
                throw new IOException("Building is not yet implemented."); // TODO

            } catch (Throwable error) {
                // we'll report this on resultReceived()
                _error = error;
            }
        }

        // this is called back on the dobj thread and must only report results
        public void resultReceived () {
            if (_error != null) {
                _roomObj.setConsole(MessageBundle.tcompose("m.build_failed", _error.getMessage()));
            } else {
                _roomObj.setConsole("m.build_complete");
            }
        }

        // this is called back on the dobj thread and must only report failure
        public void timedOut () {
            _roomObj.setConsole("m.build_timed_out");
        }

        protected int _projectId;
        protected Throwable _error;
    }

    protected ProjectRoomObject _roomObj;
}
