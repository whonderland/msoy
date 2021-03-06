//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.web.gwt.Activity;

/**
 * Contains information on a feed message.
 */
@Entity
public abstract class FeedMessageRecord extends PersistentRecord
    implements Activity
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FeedMessageRecord> _R = FeedMessageRecord.class;
    public static final ColumnExp<Integer> MESSAGE_ID = colexp(_R, "messageId");
    public static final ColumnExp<Integer> TYPE = colexp(_R, "type");
    public static final ColumnExp<String> DATA = colexp(_R, "data");
    public static final ColumnExp<Timestamp> POSTED = colexp(_R, "posted");
    // AUTO-GENERATED: FIELDS END

    /** A unique identifier for this message. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int messageId;

    /** Used to determine how to format and display the feed message. */
    public int type;

    /** A tab separated list of strings that comprise this message's data. */
    public String data;

    /** The time at which this message was posted. */
    @Index(name="ixPosted")
    public Timestamp posted;

    /**
     * Generate an add call for each member id and group id that will be required by
     * {@link #toMessage()}.
     */
    public void addReferences (Set<Integer> memberIds, Set<Integer> groupIds)
    {
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public abstract FeedMessage toMessage (
        Map<Integer, MemberName> memberNames, Map<Integer, GroupName> groupNames);

    protected FeedMessageType getType ()
    {
        return FeedMessageType.fromCode(type);
    }

    protected String[] getData ()
    {
        return data.split("\t");
    }

    protected long getPosted ()
    {
        return posted.getTime();
    }

    public long startedAt ()
    {
        return getPosted();
    }
}
