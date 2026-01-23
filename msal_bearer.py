# How to obtain a Bearer token (outline):
# 1.     Register an app in Azure AD, grant Files.ReadWrite (and offline_access if you want refresh tokens).
# For device code flow (interactive), Python example with msal

# 2.     In Azure Portal → App registrations → your app → Authentication:
# Enable “Allow public client flows”.
# Ensure a redirect for “Mobile and desktop” is present (or the default device code flow enablement).
# 2) Don’t set a client secret in the device-code script. Keep it as a PublicClientApplication with only CLIENT_ID and TENANT.
# After toggling to public client, rerun the script; you shouldn’t see the client_secret requirement. If you instead want to use a secret/cert, switch to ConfidentialClientApplication and client credential flow (but that’s app-only and different scopes, e.g., /.default).


# python3 -m pip install --upgrade msal requests
import msal, requests, json

TENANT = "334ff5d4-28bd-4c75-8431-1185bf4904a6"            # or your tenant ID
CLIENT_ID = "9b5acba6-80ce-4531-93f4-5766adc9e81a"
SCOPES = ["https://graph.microsoft.com/Files.ReadWrite"]
app = msal.PublicClientApplication(
    CLIENT_ID, authority=f"https://login.microsoftonline.com/{TENANT}"
)
flow = app.initiate_device_flow(scopes=SCOPES)
print(flow["message"])  # follow the prompt in browser
result = app.acquire_token_by_device_flow(flow)

if "access_token" in result:
    token = result["access_token"]
    print("Got token")
    # example simple upload
    file_bytes = b"hello"
    url = "https://graph.microsoft.com/v1.0/me/drive/root:/hello.txt:/content"
    resp = requests.put(url, headers={"Authorization": f"Bearer {token}"}, data=file_bytes)
    print(resp.status_code, resp.text)
else:
    print("Error:", result.get("error"), result.get("error_description"))