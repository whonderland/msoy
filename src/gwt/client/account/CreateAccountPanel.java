//
// $Id$

package client.account;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.CaptchaException;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.RegisterInfo;
import com.threerings.msoy.web.data.SessionData;

import client.shell.CShell;
import client.shell.Pages;
import client.shell.ShellMessages;
import client.shell.TrackingCookie;
import client.ui.DateFields;
import client.ui.MsoyUI;
import client.util.DateUtil;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays an interface for creating a new account.
 */
public class CreateAccountPanel extends FlowPanel
{
    public CreateAccountPanel ()
    {
        setStyleName("createAccount");

        FlowPanel loginLink = MsoyUI.createFlowPanel("Login");
        add(loginLink);
        loginLink.add(new Label(_msgs.createAlreadyMember()));
        PushButton loginButton = MsoyUI.createButton(
            MsoyUI.SHORT_THIN, _msgs.createLoginLink(),
            Link.createListener(Pages.ACCOUNT, "login"));
        loginButton.addClickListener(
            MsoyUI.createTrackingListener("signupLoginButtonClicked", null));
        loginLink.add(loginButton);

        add(MsoyUI.createLabel(_msgs.createIntro(), "Intro"));
        add(MsoyUI.createLabel(_msgs.createCoins(), "Coins"));

        add(new LabeledBox(_msgs.createEmail(), _email = new TextBox(), _msgs.createEmailTip()));
        _email.addKeyboardListener(_onType);
        Invitation invite = CShell.frame.getActiveInvitation();
        if (invite != null && invite.inviteeEmail.matches(MsoyUI.EMAIL_REGEX)) {
            // provide the invitation email as the default
            _email.setText(invite.inviteeEmail);
        }

        add(new LabeledBox(_msgs.createRealName(), _rname = new TextBox(),
                           _msgs.createRealNameTip()));
        _rname.addKeyboardListener(_onType);

        add(new LabeledBox(_msgs.createDateOfBirth(), _dateOfBirth = new DateFields(),
                           _msgs.createDateOfBirthTip()));

        add(new LabeledBox(_msgs.createPassword(), _password = new PasswordTextBox(),
                           _msgs.createPasswordTip()));
        _password.addKeyboardListener(_onType);

        add(new LabeledBox(_msgs.createConfirm(), _confirm = new PasswordTextBox(),
                           _msgs.createConfirmTip()));
        _confirm.addKeyboardListener(_onType);

        _name = MsoyUI.createTextBox("", MemberName.MAX_DISPLAY_NAME_LENGTH, -1);
        _name.addKeyboardListener(_onType);
        add(new LabeledBox(_msgs.createDisplayName(), _name, _msgs.createDisplayNameTip()));

        // optionally add the recaptcha component
        if (RecaptchaUtil.isEnabled()) {
            add(new LabeledBox(_msgs.createCaptcha(),
                               new HTML("<div id=\"recaptchaDiv\"></div>"), null));
            add(new HTML("<div id=\"recaptchaDiv\"></div>"));
        }

        add(new LabeledBox(_msgs.createTOS(), _tosBox = new CheckBox(_msgs.createTOSAgree()), null));

        HorizontalPanel controls = new HorizontalPanel();
        controls.setWidth("500px");
        controls.add(_status = MsoyUI.createLabel("", "Status"));
        controls.add(WidgetUtil.makeShim(10, 10));
        controls.setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
        ClickListener createGo = new ClickListener() {
            public void onClick (Widget sender) {
                createAccount();
            }
        };
        controls.add(MsoyUI.createButton(MsoyUI.LONG_THICK, _msgs.createGo(), createGo));
        add(controls);
    }

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        if (_email != null) {
            _email.setFocus(true);
        }
        RecaptchaUtil.init("recaptchaDiv");
    }

    protected boolean validateData ()
    {
        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        String status;
        FocusWidget toFocus = null;
        if (email.length() == 0) {
            status = _msgs.createMissingEmail();
            toFocus = _email;
        } else if (password.length() == 0) {
            status = _msgs.createMissingPassword();
            toFocus = _password;
        } else if (confirm.length() == 0) {
            status = _msgs.createMissingConfirm();
            toFocus = _confirm;
        } else if (!password.equals(confirm)) {
            status = _msgs.createPasswordMismatch();
            toFocus = _confirm;
        } else if (_dateOfBirth.getDate() == null) {
            status = _msgs.createMissingDoB();
            // this is not a FocusWidget so we have to handle it specially
            _dateOfBirth.setFocus(true);
        } else if (!MemberName.isValidDisplayName(name)) {
            status = _cmsgs.displayNameInvalid("" + MemberName.MIN_DISPLAY_NAME_LENGTH,
                "" + MemberName.MAX_DISPLAY_NAME_LENGTH);
            toFocus = _name;
        } else if (!MemberName.isValidNonSupportName(name)) {
            status = _cmsgs.nonSupportNameInvalid();
            toFocus = _name;
        } else if (!_tosBox.isChecked()) {
            status = _msgs.createMustAgreeTOS();
        } else if (RecaptchaUtil.isEnabled() && (RecaptchaUtil.getResponse() == null ||
                                                 RecaptchaUtil.getResponse().length() == 0)) {
            status = _msgs.createMustCaptcha();
            RecaptchaUtil.focus();
        } else {
            return true;
        }

        if (toFocus != null) {
            toFocus.setFocus(true);
        }
        setStatus(status);
        return false;
    }

    protected void createAccount ()
    {
        if (!validateData()) {
            return;
        }

        String[] today = new Date().toString().split(" ");
        String thirteenYearsAgo = "";
        for (int ii = 0; ii < today.length; ii++) {
            if (today[ii].matches("[0-9]{4}")) {
                int year = Integer.valueOf(today[ii]).intValue();
                today[ii] = "" + (year - 13);
            }
            thirteenYearsAgo += today[ii] + " ";
        }

        Date dob = DateUtil.toDate(_dateOfBirth.getDate());
        if (DateUtil.newDate(thirteenYearsAgo).compareTo(dob) < 0) {
            setStatus(_msgs.createNotThirteen());
            return;
        }

        RegisterInfo info = new RegisterInfo();
        info.email = _email.getText().trim();
        info.password = CShell.frame.md5hex(_password.getText().trim());
        info.displayName = _name.getText().trim();
        info.birthday = _dateOfBirth.getDate();
        info.photo = null; // TODO: remove since we're not using this any more
        info.info = new AccountInfo();
        info.info.realName = _rname.getText().trim();
        info.expireDays = 1; // TODO: unmagick?
        Invitation invite = CShell.frame.getActiveInvitation();
        info.inviteId = (invite == null) ? null : invite.inviteId;
        info.guestId = CShell.isGuest() ? CShell.getMemberId() : 0;
        info.referral = TrackingCookie.get();
        info.captchaChallenge = RecaptchaUtil.isEnabled() ? RecaptchaUtil.getChallenge() : null;
        info.captchaResponse = RecaptchaUtil.isEnabled() ? RecaptchaUtil.getResponse() : null;

        setStatus(_msgs.creatingAccount());
        _usersvc.register(DeploymentConfig.version, info, new AsyncCallback<SessionData>() {
            public void onSuccess (SessionData result) {
                result.justCreated = true;
                // let the top-level frame know that we logged on (which will trigger a redirect)
                CShell.frame.dispatchDidLogon(result);
            }
            public void onFailure (Throwable caught) {
                if (RecaptchaUtil.isEnabled()) {
                    RecaptchaUtil.reload();
                    if (caught instanceof CaptchaException) {
                        RecaptchaUtil.focus();
                    }
                }
                setStatus(CShell.serverError(caught));
            }
        });
    }

    protected void setStatus (String text)
    {
        _status.setText(text);
    }

    protected static Widget makeStep (int step, Widget contents)
    {
        SmartTable table = new SmartTable("Step", 0, 0);
        table.setText(0, 0, step + ".", 1, "Number");
        table.setWidget(0, 1, contents, 1, null);
        return table;
    }

    protected static class LabeledBox extends FlowPanel
    {
        public LabeledBox (String title, Widget contents, String tip)
        {
            setStyleName("Box");
            _tip = new SmartTable("Tip", 0, 0);
            add(title, contents, tip);
        }

        public void add (String title, final Widget contents, final String tip)
        {
            add(MsoyUI.createHTML(title, "Label"));
            add(contents);
            if (contents instanceof SourcesFocusEvents) {
                ((SourcesFocusEvents)contents).addFocusListener(new FocusListener() {
                    public void onFocus (Widget sender) {
                        // we want contents here not sender because of DateFields
                        showTip(contents, tip);
                    }
                    public void onLostFocus (Widget sender) {
                        if (_tip.isAttached()) {
                            remove(_tip);
                        }
                    }
                });
            }
        }

        protected void showTip (Widget trigger, String tip)
        {
            if (!_tip.isAttached() && tip != null) {
                DOM.setStyleAttribute(
                    _tip.getElement(), "left", (trigger.getOffsetWidth()+160) + "px");
                DOM.setStyleAttribute(
                    _tip.getElement(), "top", (trigger.getAbsoluteTop() - getAbsoluteTop() +
                    trigger.getOffsetHeight()/2 - 27) + "px");
                _tip.setText(0, 0, tip);
                add(_tip);
            }
        }

        protected SmartTable _tip;
    }

    protected KeyboardListenerAdapter _onType = new KeyboardListenerAdapter() {
        public void onKeyDown (Widget sender, char keyCode, int modifiers) {
            setStatus("");
        }
    };

    protected TextBox _email, _name, _rname;
    protected PasswordTextBox _password, _confirm;
    protected DateFields _dateOfBirth;
    protected CheckBox _tosBox;
    protected Label _status;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
