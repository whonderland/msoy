//
// $Id$

package client.people;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.gwt.ProfileServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.Page;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;
import client.util.events.PageCommandEvent;

/**
 * Displays a member's interests and other random bits.
 */
public class InterestsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.interests.size() > 0 || CShell.getMemberId() == pdata.name.getId());
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);

        _interests = pdata.interests;

        setHeader(_msgs.interestsTitle());
        displayInterests();
    }

    protected void displayInterests ()
    {
        SmartTable contents = new SmartTable("Interests", 0, 5);
        for (int ii = 0; ii < _interests.size(); ii++) {
            Interest interest = _interests.get(ii);
            contents.setText(ii, 0, _dmsgs.xlate("interest" + interest.type), 1, "Type");
            if (Interest.isLinkedType(interest.type)) {
                contents.setWidget(ii, 1, linkify(interest.interests), 1, "Text");
            } else {
                contents.setText(ii, 1, interest.interests, 1, "Text");
            }
        }
        setContent(contents);

        // display the edit button if this is our profile
        if (CShell.isSupport() || _name.getId() == CShell.getMemberId()) {
            setFooterLabel(_msgs.interestsEdit(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    startEdit();
                }
            });
            _registration = Page.register(new PageCommandEvent.Listener() {
                @Override public boolean act (PageCommandEvent commandEvent) {
                    if (commandEvent.getCommand().equals(PageCommandEvent.EDIT_INFO)) {
                        startEdit();
                        return true;
                    }
                    return false;
                }
            });
        } else {
            setFooter(null);
        }
    }

    protected void startEdit ()
    {
        unregisterForFlashCommands();

        SmartTable editor = new SmartTable("Interests", 0, 5);

        _iEditors = new TextBox[Interest.TYPES.length];

        int row = 0;
        for (int ii = 0; ii < _iEditors.length; ii++) {
            int type = Interest.TYPES[ii];
            editor.setText(row, 0, _dmsgs.xlate("interest" + type), 1, "Type");
            _iEditors[ii] = MsoyUI.createTextBox(
                getCurrentInterest(type), Interest.MAX_INTEREST_LENGTH, -1);
            _iEditors[ii].addStyleName("Editor");
            editor.setWidget(row++, 1, _iEditors[ii]);
        }

        Button cancel = new Button(_cmsgs.cancel(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                displayInterests();
            }
        });
        Button update = new Button(_cmsgs.update());
        new ClickCallback<Void>(update) {
            @Override protected boolean callService () {
                _newInterests = getNewInterests();
                _profilesvc.updateInterests(_name.getId(), _newInterests, this);
                return true;
            }

            @Override protected boolean gotResult (Void result) {
                // filter out our blank new interests
                for (int ii = 0; ii < _newInterests.size(); ii++) {
                    Interest interest = _newInterests.get(ii);
                    if (interest.interests.length() == 0) {
                        _newInterests.remove(ii--);
                    }
                }
                _interests = _newInterests;
                displayInterests();
                return true;
            }

            protected List<Interest> _newInterests;
        };

        editor.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        editor.setWidget(row++, 0, MsoyUI.createButtonPair(cancel, update), 2);

        setContent(editor);
        setFooter(null);
    }

    protected List<Interest> getNewInterests ()
    {
        List<Interest> interests = Lists.newArrayList();
        for (int ii = 0; ii < _iEditors.length; ii++) {
            Interest interest = new Interest();
            interest.type = Interest.TYPES[ii];
            interest.interests = _iEditors[ii].getText();
            interests.add(interest);
        }

        return interests;
    }

    protected String getCurrentInterest (int type)
    {
        for (int ii = 0; ii < _interests.size(); ii++) {
            Interest interest = _interests.get(ii);
            if (interest.type == type) {
                return interest.interests;
            }
        }

        return "";
    }

    protected FlowPanel linkify (String interests)
    {
        FlowPanel panel = new FlowPanel();
        String[] ivec = interests.split(",");
        for (String element2 : ivec) {
            if (panel.getWidgetCount() > 0) {
                panel.add(new InlineLabel(",", false, false, true));
            }
            String interest = element2.trim();
            panel.add(Link.create(interest, Pages.PEOPLE, "search", "0", interest));
        }

        return panel;
    }

    @Override // from Widget
    protected void onDetach ()
    {
        super.onDetach();
        unregisterForFlashCommands();
    }

    protected void unregisterForFlashCommands ()
    {
        if (_registration != null) {
            _registration.remove();
            _registration = null;
        }
    }

    protected List<Interest> _interests;
    protected TextBox[] _iEditors;
    protected Page.Registration _registration;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final ProfileServiceAsync _profilesvc = GWT.create(ProfileService.class);
}
