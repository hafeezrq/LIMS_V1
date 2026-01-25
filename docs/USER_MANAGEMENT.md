# User Management Guide

This app now enforces a password policy and supports safe user updates.

## Open User Management

1. Log in as an admin user.
2. Open: `Admin -> Manage Users`

If you are not an admin, the screen will close with an "Access Denied" message.

## Password Policy

Passwords must:

- be at least 8 characters long
- include an uppercase letter
- include a lowercase letter
- include a number
- include a symbol (for example: `!@#$%^&*`)

Example of a valid password: `Lab@2026`

## Create a User

1. Enter `Username`
2. Enter `Full Name`
3. Enter a strong `Password` (see policy above)
4. Select at least one role:
   - `ADMIN`
   - `RECEPTION`
   - `LAB`
5. Set the `Active` checkbox as needed
6. Click `Create`

## Edit a User

1. Select a user in the table
2. Click `Edit`
3. Update fields
4. Click `Create` to save changes

Important:

- The username cannot be changed.
- Leave the password blank to keep the current password.
- If you enter a password, it must follow the password policy.

## Delete a User

1. Select a user
2. Click `Delete`
3. Confirm the dialog

## Tips

- Click `Refresh` to reload the latest users from the database.
- Click `Clear` to reset the form and exit edit mode.

