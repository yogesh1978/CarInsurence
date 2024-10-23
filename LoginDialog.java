package sasva.dialogs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sasva.handlers.UserHandler;
import sasva.model.SasvaViewControllable;
import sasva.views.SideBar2;

public class LoginDialog extends Dialog {

	private Text usernameText;
	private Text passwordText;
	private boolean loginSuccessful = false;
	private Button loginButton;
	private Button logoutButton;
	private Button ssoLoginButton;
	private Button cancelButton;
	private String[] sasvaViewsIdsList = {"sasva.views.asksasva","sasva.views.chatview","sasva.views.TaskView",SideBar2.ID} ;
	

	Image backgroundImage = null;

    public LoginDialog(Shell parentShell) {
        super(parentShell);
        
        Bundle bundle = Platform.getBundle("SASVA");
        URL imageUrl = FileLocator.find(bundle, new Path("icons/sample_24x24.png"),null);
        try {
			Image image = new Image(parentShell.getDisplay(), imageUrl.openStream());
			setDefaultImage(image);
			image.dispose();
		} catch (Exception e) {
			System.out.println("Failed to load dailog icon.");
			e.printStackTrace();
		}
        
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Login to SASVA");
        shell.setSize(600, 350); // Set a fixed size for the dialog
        setCenterPosition(shell);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // No buttons will be created in the button bar
    }

    private void setCenterPosition(Shell shell) {
        Point size = shell.getSize();
        Shell parent = getParentShell();
        int x = (parent.getDisplay().getBounds().width - size.x) / 2;
        int y = (parent.getDisplay().getBounds().height - size.y) / 2;
        shell.setLocation(x, y);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(2, false); // Two columns for LHS and RHS
        container.setLayout(layout);

		// Left-hand side: Background image (40% width)
		Composite leftComposite = new Composite(container, SWT.NONE);
		GridData leftGridData = new GridData(SWT.FILL, SWT.FILL, false, true);
		leftGridData.widthHint = 240; // 40% of 800px
		leftComposite.setLayoutData(leftGridData);

		try {
			Bundle bundle = Platform.getBundle("SASVA");
			backgroundImage = new Image(Display.getCurrent(),
					FileLocator.openStream(bundle, new Path("/icons/SASVA-scaled.png"), false));
			leftComposite.setBackgroundImage(backgroundImage);
			leftComposite.setBackgroundMode(SWT.INHERIT_DEFAULT);
		} catch (IOException e) {
			System.err.println("Failed to load background image: " + e.getMessage());
			e.printStackTrace();
		}

		// Right-hand side: Login form (60% width)
		Composite rightComposite = new Composite(container, SWT.BORDER);
		GridData rightGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		rightComposite.setLayoutData(rightGridData);
		rightComposite.setLayout(new GridLayout(1, false));
		if (UserHandler.isUserLoggedIn()) {
			new Label(rightComposite, SWT.NONE).setText("You are logged in.");
			Composite buttonComposite = new Composite(rightComposite, SWT.NONE);
			buttonComposite.setLayout(new GridLayout(2, true));
			// Cancel Button
			cancelButton = new Button(buttonComposite, SWT.PUSH);
			cancelButton.setText("Cancel");
			cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			cancelButton.setSize(100, 30); // Adjust button size
			cancelButton.addListener(SWT.Selection, e -> close()); // Close the dialog
			// Logtin Button
			logoutButton = new Button(buttonComposite, SWT.PUSH);
			logoutButton.setText("Logout");
			logoutButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			logoutButton.setSize(100, 30); // Adjust button size
			logoutButton.addListener(SWT.Selection, e -> handleLogout());
			return container;
		}
        // Username and Password Fields
        new Label(rightComposite, SWT.NONE).setText("Email ID:");
        usernameText = new Text(rightComposite, SWT.BORDER);
        GridData usernameGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        usernameGridData.widthHint = 150; // Reduced width
        usernameText.setLayoutData(usernameGridData);
        

        new Label(rightComposite, SWT.NONE).setText("Password:");
        passwordText = new Text(rightComposite, SWT.BORDER | SWT.PASSWORD);
        GridData passwordGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        passwordGridData.widthHint = 150; // Reduced width
        passwordText.setLayoutData(passwordGridData);
        
        //setting default user email in username text box, if present.
        UserHandler handler = new UserHandler();
        if(handler.retrieveEmail() != null && !handler.retrieveEmail().isBlank() && !handler.retrieveEmail().isEmpty()) {
        	usernameText.setText(handler.retrieveEmail());
        	usernameText.setSelection(usernameText.getText().length());
        	passwordText.setFocus();
        }
        // Add a row for spacing before buttons
        //new Label(rightComposite, SWT.NONE).setText(""); // Empty label for spacing

        // Button Composite for Login and Cancel buttons
        Composite buttonComposite = new Composite(rightComposite, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, true)); // Two buttons with equal space

        // Login Button
        loginButton = new Button(buttonComposite, SWT.PUSH);
        loginButton.setText("Login");
        loginButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        loginButton.setEnabled(false); // Initially disabled
        loginButton.setSize(100, 30); // Adjust button size
        loginButton.addListener(SWT.Selection, e -> handleLogin());

        // Cancel Button
        cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setText("Cancel");
        cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        cancelButton.setSize(100, 30); // Adjust button size
        cancelButton.addListener(SWT.Selection, e -> close()); // Close the dialog

        // Add event listeners to enable the login button based on input validation
        usernameText.addModifyListener(e -> validateInputs());
        passwordText.addModifyListener(e -> validateInputs());

        // Add space between buttons and SSO button
        //new Label(rightComposite, SWT.NONE).setText(""); // Empty label for spacing

        // Separator between login form and SSO button
//        Label separator = new Label(rightComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
//        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//        new Label(rightComposite, SWT.NONE).setText(""); // Empty label for spacing        
//        // SSO Button
//        ssoLoginButton = new Button(rightComposite, SWT.PUSH);
//        ssoLoginButton.setText("SSO Login");
//        ssoLoginButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//        ssoLoginButton.setSize(100, 30); // Adjust button size
//        ssoLoginButton.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_CYAN)); // Set background color to white
//        ssoLoginButton.addListener(SWT.Selection, e -> performSSOAuth());

        return container;
		}

	public void handleLogout() {
		IPreferenceStore store = PlatformUI.getPreferenceStore();
		loginSuccessful = false;
		store.setValue("isUserLoggedIn", false);
		UserHandler handler = new UserHandler();
		handler.storeToken("");
		handler.storeRefreshToken("");
		closeViews();
//		logout();
		close();
	}

	private void closeViews() {
		System.out.println("Closing opened pages");
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			IViewPart view;
			for(String viewsId : sasvaViewsIdsList) {
				view = page.findView(viewsId);
				if (view != null && view instanceof SasvaViewControllable) {
					SasvaViewControllable disableViews = (SasvaViewControllable) view;
					disableViews.disableControls();
				}
			}			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void logout() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			IPreferenceStore store = PlatformUI.getPreferenceStore();
			UserHandler userHandler = new UserHandler();
			String logoutUrl = "";
			String requestMethod = "POST";
			String authorization = "";
			String configString = store.getString("sasvaApiResponse");
			JsonNode configJsonNode;

			configJsonNode = mapper.readTree(configString);

			for (JsonNode config : configJsonNode) {
				if (config.get("signout") != null) {
					logoutUrl = config.get("signout").asText();
				}
			}
			if (logoutUrl.isBlank() || logoutUrl.isEmpty()) {
				// message on page
				System.out.println("Signout Url not found");
				return;
			}
			HttpURLConnection connection = null;
			StringBuilder response = new StringBuilder();
			try {
				URL url = new URL(logoutUrl);
				connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod(requestMethod);
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setDoOutput(true);
				connection.setRequestProperty("Authorization", authorization);
				// Read the response
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(connection.getInputStream(), "utf-8"))) {
					String responseLine;
					while ((responseLine = br.readLine()) != null) {
						response.append(responseLine.trim());
					}
				}
				JsonNode responseJsonNode = mapper.readTree(response.toString());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}

			removeViews();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void removeViews() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if(page != null) {
			IViewPart asksasva = page.findView("sasva.views.asksasva");
			if(asksasva != null) {
				page.hideView(asksasva);
				System.out.println("removing asksasva");
			}
			IViewPart chatview = page.findView("sasva.views.chatview");
			if(chatview != null) {
				page.hideView(chatview);
				System.out.println("removing chatview");
			}
		}
		
	}

	@Override
	protected void okPressed() {
		// This method is no longer needed since we removed the default OK button
		// functionality
	}

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }

    private void validateInputs() {
        boolean enable = !usernameText.getText().isEmpty() && !passwordText.getText().isEmpty();
        loginButton.setEnabled(enable);
    }

    private void handleLogin() {
        boolean isAuthenticated = authenticate(usernameText.getText(), passwordText.getText());

        if (isAuthenticated) {
            if (backgroundImage != null && !backgroundImage.isDisposed()) {
                backgroundImage.dispose();
            }
            loginSuccessful = true;
			enableViews();
			close(); // Close the dialog
		} else {
			loginSuccessful = false;
			MessageBox authFailedMessageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
			authFailedMessageBox.setText("Error");
			IPreferenceStore store = PlatformUI.getPreferenceStore();
			try {
				ObjectMapper mapper = new ObjectMapper();
				String configString = store.getString("sasvaApiResponse");
				JsonNode configJsonNode = mapper.readTree(configString);
				String signinUrl = "";
				for (JsonNode config : configJsonNode) {
					if (config.get("signin") != null) {
						signinUrl = config.get("signin").asText();
					}
				}
				if (signinUrl.isBlank() || signinUrl.isEmpty()) {
					authFailedMessageBox.setMessage("Sasva settings missing. Use preferences to save/update them.");
				} else {
					authFailedMessageBox.setMessage("Invalid credentials provided.");
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
				authFailedMessageBox.setMessage("Invalid credentials provided.");
			}
			authFailedMessageBox.open();
		}
	}

	private void enableViews() {
		System.out.println("Enabling opened pages");
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			IViewPart view;
			for (String viewsId : sasvaViewsIdsList) {
				view = page.findView(viewsId);
				if (view != null && view instanceof SasvaViewControllable) {
					SasvaViewControllable disableViews = (SasvaViewControllable) view;
					disableViews.enableControls();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private boolean authenticate(String username, String password) {
		Map<String, String> payload = new HashMap<String, String>();
		IPreferenceStore store = PlatformUI.getPreferenceStore();
		UserHandler userHandler = new UserHandler();
		String encodedPassword =  Base64.getEncoder().encodeToString(password.getBytes());
		payload.put("email", username);
		store.setValue("userEmail", username);
		payload.put("password", encodedPassword);
		try {
			ObjectMapper mapper = new ObjectMapper();
			String jsonString = mapper.writeValueAsString(payload);

			String configString = store.getString("sasvaApiResponse");
			JsonNode configJsonNode = mapper.readTree(configString);
			String signinUrl = "";
			for (JsonNode config : configJsonNode) {
				if (config.get("signin") != null) {
					signinUrl = config.get("signin").asText();
				}
			}
			if (signinUrl.isBlank() || signinUrl.isEmpty()) {
				// message on page
				System.out.println("Signin Url not found");
			}
			Map<String, String> responseMap = userHandler.getResponse(signinUrl, "POST", jsonString);
			if (responseMap.get("statusCode").equals("200")) {
//				userHandler.storeToken(mapper.readTree(responseMap.get("data")).get("token").asText());
//				System.out.println("token: "+userHandler.retrieveToken()+"  "+responseMap.get("data"));
				store.setValue("isUserLoggedIn", true);
				return true;
			} else {
				System.err.println(responseMap.get("response"));

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		store.setValue("isUserLoggedIn", false);
		return false;
	}

    private void performSSOAuth() {
        boolean isAuthenticated = simulateSSOAuth();

        if (isAuthenticated) {
            loginSuccessful = true;
            if (backgroundImage != null && !backgroundImage.isDisposed()) {
                backgroundImage.dispose();
            }
            close(); // Close the dialog
        } else {
            loginSuccessful = false;
            MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            messageBox.setMessage("SSO authentication failed.");
            messageBox.open();
        }
    }

    private boolean simulateSSOAuth() {
        return true; // Placeholder for actual SSO check
    }
    
    private void disposeResources() {
        // Dispose of the background image
        if (backgroundImage != null && !backgroundImage.isDisposed()) {
            backgroundImage.dispose();
        }
    }
    
}
