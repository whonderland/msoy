//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;

/**
 * A class that will prompt the user, and will call one of two abstract functions, depending
 * on the option selected.
 */
public abstract class PromptPopup extends BorderedPopup
{
    /**
     * Create a PromptPopup that uses "Yes" and "No" for the buttons.
     */
    public PromptPopup (String prompt)
    {
        this(prompt, "Yes", "No");
    }

    /**
     * @param prompt The string to prompt the user with.  This string cannot contain HTML.
     * @param affirmative The text to use on the "true" button.
     * @param negative The text to use on the "false" button.
     */
    public PromptPopup (String prompt, String affirmative, String negative)
    {
        super(false);
        _prompt = prompt;
        _affirmative = affirmative;
        _negative = negative;
    }

    /**
     * Prompt the user, call onAffirmative or onNegative depending on user's input.
     */
    public void prompt () 
    {
        VerticalPanel content = new VerticalPanel();
        content.setStyleName("promptPopup");

        Label headerLabel = new Label(_prompt);
        headerLabel.setStyleName("Header");
        content.add(headerLabel);
        content.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        final Button yesButton = new Button(_affirmative);
        final Button noButton = new Button(_negative);
        ClickListener listener = new ClickListener () {
            public void onClick (Widget sender) {
                if (sender == yesButton) {
                    onAffirmative();
                } else {
                    onNegative();
                }
                hide();
            }  
        };
        yesButton.addClickListener(listener);
        noButton.addClickListener(listener);
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(10);
        buttons.add(yesButton);
        buttons.add(noButton);
        content.add(buttons);

        setWidget(content);
        show();
    }

    /**
     * Called if the user selects the affirmative option.
     */
    public abstract void onAffirmative ();

    /**
     * Called if the user selects the negative option.
     */
    public abstract void onNegative ();

    protected String _prompt;
    protected String _affirmative;
    protected String _negative;
}
