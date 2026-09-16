
private void loadUserProfile(String uid) {

    // First: try the normal Firebase UID document.
    db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener(document -> {

                if (document.exists()) {
                    openUserProfile(document);
                    return;
                }

                // Second: some existing Sakay Na accounts may have
                // their phone number stored as the document ID instead
                // of the Firebase UID.
                String phone = normalizePhone(phoneField != null
                        ? phoneField.getText().toString()
                        : "");

                if (phone.isEmpty()) {
                    showUserNotFound();
                    return;
                }

                db.collection("users")
                        .whereEqualTo("phone", phone)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(query -> {

                            if (!query.isEmpty()) {
                                openUserProfile(query.getDocuments().get(0));
                            } else {
                                showUserNotFound();
                            }

                        })
                        .addOnFailureListener(e -> {

                            showError(
                                    "Unable to find your account: "
                                            + e.getMessage()
                            );
                        });

            })
            .addOnFailureListener(e -> {

                showError(
                        "Unable to load user profile: "
                                + e.getMessage()
                );
            });
}

private void openUserProfile(
        com.google.firebase.firestore.DocumentSnapshot document) {

    String role = document.getString("role");

    if (role == null) {
        auth.signOut();
        buildLoginScreen();

        Toast.makeText(
                this,
                "User role is not found.",
                Toast.LENGTH_LONG
        ).show();

        return;
    }

    role = role.trim().toUpperCase();

    // ADMIN
    if (role.equals("ADMIN")) {
        openActivity(AdminActivity.class);
        return;
    }

    // DRIVER
    if (role.equals("DRIVER")) {

        Boolean approved = document.getBoolean("approved");

        if (approved == null) {
            approved = false;
        }

        if (!approved) {
            auth.signOut();
            buildLoginScreen();

            Toast.makeText(
                    this,
                    "Driver account is waiting for Admin approval.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        openActivity(DriverActivity.class);
        return;
    }

    // PASSENGER
    if (role.equals("PASSENGER")) {
        openActivity(PassengerActivity.class);
        return;
    }

    auth.signOut();
    buildLoginScreen();

    Toast.makeText(
            this,
            "Unknown account role: " + role,
            Toast.LENGTH_LONG
    ).show();
}

private void showUserNotFound() {

    auth.signOut();
    buildLoginScreen();

    Toast.makeText(
            this,
            "User not found. Check your phone number and password.",
            Toast.LENGTH_LONG
    ).show();
}
