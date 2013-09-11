package edu.wwu.cs412.tvfanatic.account;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import edu.wwu.cs412.tvfanatic.R;
import edu.wwu.cs412.tvfanatic.account.Account.LoginListener;
import edu.wwu.cs412.tvfanatic.http.AsyncTaskCompleteListener;

/**
 * A dialog that appears when the user first runs the app. In its current state, it simple prompts for an
 * e-mail address and a 'display name'. Eventually, we want to tie this into Google's authentication service.
 */
public class AccountCreateDialog extends DialogFragment implements OnClickListener {
	private LoginListener listener;
	private EditText emailEdit;
	private EditText displayNameEdit;
	private String emailString;
	
	public static AccountCreateDialog newInstance(LoginListener listener, String email) {
		AccountCreateDialog dlg = new AccountCreateDialog(email);
		dlg.setCancelable(false);
		dlg.listener = listener;
		return dlg;
	}
	
	private AccountCreateDialog(String email) {
		emailString = email;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getDialog().setTitle("Create your T.V. Fanatic account");
		
		View v = inflater.inflate(R.layout.account_create_dialog, container, false);
		emailEdit = (EditText) v.findViewById(R.id.account_create_email_edit);
		if (emailString != null) {
			// Use default gmail account
			emailEdit.setText(emailString);
			emailEdit.setEnabled(false);
			emailEdit.setFocusable(false);
			emailEdit.setFocusableInTouchMode(false);
		} else {
			// Let user set email
			emailEdit.setEnabled(true);
			emailEdit.setFocusable(true);
			emailEdit.setFocusableInTouchMode(true);
		}
		displayNameEdit = (EditText) v.findViewById(R.id.account_create_display_name_edit);
		
		Button btnSignUp = (Button) v.findViewById(R.id.account_create_sign_up_button);
		btnSignUp.setOnClickListener(this);
		
		return v;
	}

	public void onClick(View v) {
		String email = emailEdit.getText().toString();
		String displayName = displayNameEdit.getText().toString();
		boolean allowComments = true; // Set to default value (not editable in this dialog)
		
		// Server logs in new user automatically
		Account.create(getActivity(), email, displayName, allowComments, listener);
		getDialog().dismiss();
	}
}
