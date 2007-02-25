//
// $Id$

package com.threerings.msoy.admin.data;

import com.threerings.admin.data.ConfigObject;

/**
 * Contains runtime configurable general server configuration.
 */
public class ServerConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>nonAdminsAllowed</code> field. */
    public static final String NON_ADMINS_ALLOWED = "nonAdminsAllowed";

    /** The field name of the <code>dailyFlowEvaporation</code> field. */
    public static final String DAILY_FLOW_EVAPORATION = "dailyFlowEvaporation";

    /** The field name of the <code>abuseFactorReassessment</code> field. */
    public static final String ABUSE_FACTOR_REASSESSMENT = "abuseFactorReassessment";

    /** The field name of the <code>humanityReassessment</code> field. */
    public static final String HUMANITY_REASSESSMENT = "humanityReassessment";
    // AUTO-GENERATED: FIELDS END

    /** Whether or not to allow non-admins to log on. */
    public boolean nonAdminsAllowed = true;

    /** The fraction of a user's flow that evaporates over a 24-hour period. */
    public float dailyFlowEvaporation = 0.20f;

    /** The number of player minutes between reassessments of a game's anti-abuse factor. */
    public int abuseFactorReassessment = 1000;

    /** The number of seconds between reassessments of a member's humanity factor. */
    public int humanityReassessment = 24 * 3600;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>nonAdminsAllowed</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNonAdminsAllowed (boolean value)
    {
        boolean ovalue = this.nonAdminsAllowed;
        requestAttributeChange(
            NON_ADMINS_ALLOWED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.nonAdminsAllowed = value;
    }

    /**
     * Requests that the <code>dailyFlowEvaporation</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setDailyFlowEvaporation (float value)
    {
        float ovalue = this.dailyFlowEvaporation;
        requestAttributeChange(
            DAILY_FLOW_EVAPORATION, Float.valueOf(value), Float.valueOf(ovalue));
        this.dailyFlowEvaporation = value;
    }

    /**
     * Requests that the <code>abuseFactorReassessment</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAbuseFactorReassessment (int value)
    {
        int ovalue = this.abuseFactorReassessment;
        requestAttributeChange(
            ABUSE_FACTOR_REASSESSMENT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.abuseFactorReassessment = value;
    }

    /**
     * Requests that the <code>humanityReassessment</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setHumanityReassessment (int value)
    {
        int ovalue = this.humanityReassessment;
        requestAttributeChange(
            HUMANITY_REASSESSMENT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.humanityReassessment = value;
    }
    // AUTO-GENERATED: METHODS END
}
