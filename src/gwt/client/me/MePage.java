//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.Page;
import client.util.Link;

public class MePage extends Page
{
    public static final String TRANSACTIONS = "transactions";
    public static final String DEVIANT_CONTEST = "dacontesti";
    public static final String CONTESTS = "contests";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        if (action.equals("passport")) {
            // guests should never get a link to a passport page that will use the default, so 0
            // is fine (it'll through an internal error on the server)
            int defaultId = CShell.creds == null ? 0 : CShell.creds.name.getMemberId();
            setContent(_msgs.titlePassport(), new PassportPanel(args.get(1, defaultId)));

        } else if (action.equals("medals")) {
            int defaultId = CShell.creds == null ? 0 : CShell.creds.name.getMemberId();
            setContent(_msgs.titlePassport(),
                new PassportPanel(args.get(1, defaultId), PassportPanel.Content.MEDALS));

        } else if (DeploymentConfig.devDeployment && action.equals("passportimagetest")) {
            setContent(_msgs.titlePassportTest(), new PassportImageTestPanel());

        } else if (action.equals(TRANSACTIONS)) {
            int report = args.get(1, 1);
            int memberId = args.get(2, CShell.getMemberId());
            setContent(_msgs.transactionsTitle(), new TransactionsPanel(report, memberId));

        } else if (action.equals(CONTESTS)) {
            setContent(_msgs.titleContests(), new ContestsPanel());

        } else if (action.equals(DEVIANT_CONTEST)) {
            Link.go(Pages.LANDING, "dawinners");

        } else if (CShell.isPermaguest()) {
            Link.go(Pages.ACCOUNT, "create");

        } else if (CShell.isPermaguest()) {
            Link.go(Pages.ACCOUNT, "edit"); // permaguest must first register

        } else if (CShell.isGuest()) {
            Link.go(Pages.ACCOUNT, "create"); // redirect to create/logon page

        } else {
            setContent(new MyWhirled());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.ME;
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
