package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeomorphicButton
import com.example.ui.components.NeomorphicIconButton
import com.example.ui.components.NeomorphicTextField
import com.example.ui.theme.SilkBackground
import com.example.ui.theme.SilkOnSurface
import com.example.ui.theme.SilkOnSurfaceVariant
import com.example.ui.theme.SilkPrimary
import com.example.ui.viewmodel.AilaMapViewModel
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown

@Composable
fun LoginScreen(
    viewModel: AilaMapViewModel,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    
    // Google Sign-In Simulation State Variables
    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleStep by remember { mutableStateOf(1) } // 1: Email, 2: Password
    var googleEmail by remember { mutableStateOf("") }
    var googlePassword by remember { mutableStateOf("") }
    var googleEmailError by remember { mutableStateOf("") }
    var googlePasswordError by remember { mutableStateOf("") }
    var showGooglePassword by remember { mutableStateOf(false) }
    
    val onDismissGoogle = {
        showGoogleDialog = false
        googleStep = 1
        googleEmail = ""
        googlePassword = ""
        googleEmailError = ""
        googlePasswordError = ""
        showGooglePassword = false
    }
    
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SilkBackground)
            .padding(24.dp)
            .testTag("login_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background decorative soft circular shapes (Neomorphic clay art)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-60).dp)
                .size(240.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.4f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 80.dp)
                .size(280.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.03f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            // Main Hero Logo Branding
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_aila_logo),
                contentDescription = "Aila Maps Logo",
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Navigate with precision",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = SilkOnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Inline Validation Error banner
            if (validationError.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = validationError,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Credentials Fields Panel
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isSignUpMode) {
                    // Full Name field for Sign Up
                    Text(
                        text = "Full Name",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SilkOnSurfaceVariant
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    NeomorphicTextField(
                        value = fullName,
                        onValueChange = { fullName = it; validationError = "" },
                        placeholder = "John Doe",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "User Icon",
                                tint = SilkOnSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        testTag = "fullname_input"
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Email field
                Text(
                    text = "Email Address",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SilkOnSurfaceVariant
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )

                NeomorphicTextField(
                    value = email,
                    onValueChange = { email = it; validationError = "" },
                    placeholder = "name@example.com",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = "Mail Icon",
                            tint = SilkOnSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "email_input"
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Password field Label Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SilkOnSurfaceVariant
                        )
                    )
                    if (!isSignUpMode) {
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = SilkPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable {
                                    viewModel.loginWithGoogleSimulation("recovered@ailamaps.com", rememberMe)
                                }
                                .padding(4.dp)
                        )
                    }
                }

                NeomorphicTextField(
                    value = password,
                    onValueChange = { password = it; validationError = "" },
                    placeholder = "••••••••",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Lock Icon",
                            tint = SilkOnSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isSignUpMode) {
                                focusManager.clearFocus()
                                if (email.isNotEmpty()) {
                                    viewModel.loginWithGoogleSimulation(email, rememberMe)
                                } else {
                                    validationError = "Please fill in email address"
                                }
                            }
                        }
                    ),
                    testTag = "password_input"
                )

                if (isSignUpMode) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Confirm Password",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SilkOnSurfaceVariant
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    NeomorphicTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; validationError = "" },
                        placeholder = "••••••••",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Lock Icon",
                                tint = SilkOnSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                                    validationError = "Please fill in all details"
                                } else if (password != confirmPassword) {
                                    validationError = "Passwords do not match!"
                                } else {
                                    viewModel.loginWithGoogleSimulation(email, rememberMe)
                                }
                            }
                        ),
                        testTag = "confirm_password_input"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Remember Me Toggle Button / Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .clickable { rememberMe = !rememberMe }
                    .testTag("remember_me_row")
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (rememberMe) SilkPrimary else SilkBackground)
                        .border(1.5.dp, if (rememberMe) SilkPrimary else SilkOnSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (rememberMe) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Checked",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Remember Me (Keep Logged In)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SilkOnSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Grouping
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = SilkPrimary)
                }
            } else {
                NeomorphicButton(
                    onClick = {
                        focusManager.clearFocus()
                        if (email.trim().isEmpty() || !email.contains("@")) {
                            validationError = "Please enter a valid email address."
                            return@NeomorphicButton
                        }
                        if (password.trim().length < 6) {
                            validationError = "Password must be at least 6 characters long."
                            return@NeomorphicButton
                        }

                        isLoading = true
                        validationError = ""
                        
                        if (isSignUpMode) {
                            if (fullName.trim().isEmpty()) {
                                validationError = "Please enter your full name."
                                isLoading = false
                                return@NeomorphicButton
                            }
                            if (password != confirmPassword) {
                                validationError = "Passwords do not match."
                                isLoading = false
                                return@NeomorphicButton
                            }
                        }

                        viewModel.loginWithFirebase(email.trim(), password, isSignUpMode, rememberMe) { success, errorMsg ->
                            isLoading = false
                            if (!success) {
                                validationError = errorMsg ?: "Connection error. Failed to reach Firebase."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    testTag = "login_button"
                ) {
                    Text(
                        text = if (isSignUpMode) "Register & Explore" else "Sign In",
                        style = MaterialTheme.typography.titleMedium.copy(color = SilkPrimary, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Submit",
                        tint = SilkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Google Sign-In Simulator Button
            NeomorphicButton(
                onClick = {
                    focusManager.clearFocus()
                    showGoogleDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                backgroundColor = Color.White,
                testTag = "google_signin_button"
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Google Icon",
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign In with Google",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF1F1F1F),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider or OR
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(SilkOnSurfaceVariant.copy(alpha = 0.15f)))
                Text(
                    text = "OR",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = SilkOnSurfaceVariant.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(SilkOnSurfaceVariant.copy(alpha = 0.15f)))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue as guest tactile button
            NeomorphicButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.continueAsGuest(rememberMe)
                },
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                backgroundColor = SilkBackground,
                testTag = "guest_login_button"
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Guest Usage",
                    tint = SilkOnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue as Guest",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SilkOnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Made by Aila Company Solution",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SilkOnSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Google high-fidelity Sign-In Simulator Dialog
            if (showGoogleDialog) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = onDismissGoogle,
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF0F4F9)) // Light Google gray-blue
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .widthIn(max = 740.dp)
                                .wrapContentHeight()
                        ) {
                            val isWide = maxWidth >= 620.dp
                            
                            // Card Container
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(28.dp))
                                    .padding(if (isWide) 40.dp else 24.dp)
                            ) {
                                // Close button at top corner
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    androidx.compose.material3.IconButton(
                                        onClick = onDismissGoogle,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Close",
                                            tint = Color(0xFF444746)
                                        )
                                    }
                                }

                                if (isWide) {
                                    // Wide Side-By-Side Google Web Sign In Style Layout
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(40.dp)
                                    ) {
                                        // Left Column: Branding and Header
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            GoogleGLogo(modifier = Modifier.size(40.dp))
                                            
                                            if (googleStep == 1) {
                                                Text(
                                                    text = "Sign in",
                                                    fontSize = 32.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = Color(0xFF1F1F1F)
                                                )
                                                Text(
                                                    text = "Use your Google Account",
                                                    fontSize = 16.sp,
                                                    color = Color(0xFF444746),
                                                    fontWeight = FontWeight.Normal
                                                )
                                            } else {
                                                Text(
                                                    text = "Welcome",
                                                    fontSize = 32.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = Color(0xFF1F1F1F)
                                                )
                                                
                                                // Real Selected Account Pill
                                                Row(
                                                    modifier = Modifier
                                                        .border(1.dp, Color(0xFFC4C7C5), RoundedCornerShape(100.dp))
                                                        .clickable { googleStep = 1 }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.AccountCircle,
                                                        contentDescription = "Account icon",
                                                        tint = Color(0xFF5f6368),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = googleEmail,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF1F1F1F),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "▼",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF5f6368)
                                                    )
                                                }
                                            }
                                        }

                                        // Right Column: Input form controls & Buttons
                                        Column(
                                            modifier = Modifier.weight(1.1f),
                                            verticalArrangement = Arrangement.spacedBy(24.dp)
                                        ) {
                                            if (googleStep == 1) {
                                                GoogleEmailFormSection(
                                                    googleEmail = googleEmail,
                                                    onEmailChange = { googleEmail = it; googleEmailError = "" },
                                                    googleEmailError = googleEmailError,
                                                    onNext = {
                                                        val trimmed = googleEmail.trim()
                                                        if (trimmed.isEmpty()) {
                                                            googleEmailError = "Enter an email or phone number"
                                                        } else if (!trimmed.contains("@") || trimmed.length < 5) {
                                                            googleEmailError = "Could not find your Google Account"
                                                        } else {
                                                            googleStep = 2
                                                        }
                                                    }
                                                )
                                            } else {
                                                GooglePasswordFormSection(
                                                    googlePassword = googlePassword,
                                                    onPasswordChange = { googlePassword = it; googlePasswordError = "" },
                                                    googlePasswordError = googlePasswordError,
                                                    showGooglePassword = showGooglePassword,
                                                    onShowPasswordToggle = { showGooglePassword = it },
                                                    onBack = { googleStep = 1 },
                                                    onNext = {
                                                        val pswd = googlePassword.trim()
                                                        if (pswd.isEmpty()) {
                                                            googlePasswordError = "Enter a password"
                                                        } else if (pswd.length < 6) {
                                                            googlePasswordError = "Wrong password. Try again or click Forgot password"
                                                        } else {
                                                            showGoogleDialog = false
                                                            viewModel.loginWithGoogleSimulation(googleEmail.trim(), rememberMe)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Compact Column Mobile Style Layout
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        GoogleGLogo(modifier = Modifier.size(36.dp))
                                        
                                        if (googleStep == 1) {
                                            Text(
                                                text = "Sign in",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF1F1F1F)
                                            )
                                            Text(
                                                text = "Use your Google Account",
                                                fontSize = 15.sp,
                                                color = Color(0xFF444746),
                                                fontWeight = FontWeight.Normal
                                            )
                                        } else {
                                            Text(
                                                text = "Welcome",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF1F1F1F)
                                            )
                                            
                                            // Account Pill Header
                                            Row(
                                                modifier = Modifier
                                                    .border(1.dp, Color(0xFFC4C7C5), RoundedCornerShape(100.dp))
                                                    .clickable { googleStep = 1 }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.AccountCircle,
                                                    contentDescription = "Account icon",
                                                    tint = Color(0xFF5f6368),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = googleEmail,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF1F1F1F),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "▼",
                                                    fontSize = 8.sp,
                                                    color = Color(0xFF5f6368)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (googleStep == 1) {
                                            GoogleEmailFormSection(
                                                googleEmail = googleEmail,
                                                onEmailChange = { googleEmail = it; googleEmailError = "" },
                                                googleEmailError = googleEmailError,
                                                onNext = {
                                                    val trimmed = googleEmail.trim()
                                                    if (trimmed.isEmpty()) {
                                                        googleEmailError = "Enter an email or phone number"
                                                    } else if (!trimmed.contains("@") || trimmed.length < 5) {
                                                        googleEmailError = "Could not find your Google Account"
                                                    } else {
                                                        googleStep = 2
                                                    }
                                                }
                                            )
                                        } else {
                                            GooglePasswordFormSection(
                                                googlePassword = googlePassword,
                                                onPasswordChange = { googlePassword = it; googlePasswordError = "" },
                                                googlePasswordError = googlePasswordError,
                                                showGooglePassword = showGooglePassword,
                                                onShowPasswordToggle = { showGooglePassword = it },
                                                onBack = { googleStep = 1 },
                                                onNext = {
                                                    val pswd = googlePassword.trim()
                                                    if (pswd.isEmpty()) {
                                                        googlePasswordError = "Enter a password"
                                                    } else if (pswd.length < 6) {
                                                        googlePasswordError = "Wrong password. Try again or click Forgot password"
                                                    } else {
                                                        showGoogleDialog = false
                                                        viewModel.loginWithGoogleSimulation(googleEmail.trim(), rememberMe)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Footer link
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable {
                        isSignUpMode = !isSignUpMode
                        validationError = ""
                    }
                    .padding(8.dp)
            ) {
                Text(
                    text = if (isSignUpMode) "Already have an account? Sign In" else "Don't have an account? Create Account",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SilkPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// ==========================================
// GOOGLE HIGH-FIDELITY SIMULATOR COMPONENTS
// ==========================================

@Composable
fun GoogleGLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sizeMin = size.minDimension
        val scale = sizeMin / 24f
        scale(scale, scale, androidx.compose.ui.geometry.Offset.Zero) {
            // Blue sector
            val pBlue = androidx.compose.ui.graphics.Path().apply {
                moveTo(22.56f, 12.25f)
                cubicTo(22.56f, 11.47f, 22.49f, 10.72f, 22.36f, 10.0f)
                lineTo(12.0f, 10.0f)
                lineTo(12.0f, 14.26f)
                lineTo(17.92f, 14.26f)
                cubicTo(17.66f, 15.63f, 16.88f, 16.79f, 15.71f, 17.57f)
                lineTo(15.71f, 20.34f)
                lineTo(19.28f, 20.34f)
                cubicTo(21.36f, 18.42f, 22.56f, 15.6f, 22.56f, 12.25f)
                close()
            }
            drawPath(pBlue, Color(0xFF4285F4))

            // Green sector
            val pGreen = androidx.compose.ui.graphics.Path().apply {
                moveTo(12.0f, 23.0f)
                cubicTo(14.97f, 23.0f, 17.46f, 22.02f, 19.28f, 20.34f)
                lineTo(15.71f, 17.57f)
                cubicTo(14.73f, 18.23f, 13.48f, 18.63f, 12.0f, 18.63f)
                cubicTo(9.14f, 18.63f, 6.71f, 16.7f, 5.84f, 14.1f)
                lineTo(2.18f, 14.1f)
                lineTo(2.18f, 16.94f)
                cubicTo(3.99f, 20.53f, 7.7f, 23.00f, 12.0f, 23.00f)
                close()
            }
            drawPath(pGreen, Color(0xFF34A853))

            // Yellow sector
            val pYellow = androidx.compose.ui.graphics.Path().apply {
                moveTo(5.84f, 14.1f)
                cubicTo(5.62f, 13.44f, 5.49f, 12.74f, 5.49f, 12.0f)
                cubicTo(5.49f, 11.27f, 5.62f, 10.56f, 5.84f, 9.9f)
                lineTo(5.84f, 7.06f)
                lineTo(2.18f, 7.06f)
                cubicTo(1.43f, 8.55f, 1.0f, 10.22f, 1.0f, 12.0f)
                cubicTo(1.0f, 13.78f, 1.43f, 15.45f, 2.18f, 16.94f)
                lineTo(5.84f, 14.1f)
                close()
            }
            drawPath(pYellow, Color(0xFFFBBC05))

            // Red sector
            val pRed = androidx.compose.ui.graphics.Path().apply {
                moveTo(12.0f, 5.38f)
                cubicTo(14.86f, 5.38f, 17.29f, 7.31f, 18.16f, 9.91f)
                lineTo(21.82f, 7.07f)
                cubicTo(20.01f, 3.48f, 16.3f, 1.0f, 12.0f, 1.0f)
                cubicTo(7.7f, 1.0f, 3.99f, 3.48f, 2.18f, 7.07f)
                lineTo(5.84f, 9.91f)
                cubicTo(6.71f, 7.31f, 9.14f, 5.38f, 12.0f, 5.38f)
                close()
            }
            drawPath(pRed, Color(0xFFEA4335))
        }
    }
}

@Composable
fun GoogleEmailFormSection(
    googleEmail: String,
    onEmailChange: (String) -> Unit,
    googleEmailError: String,
    onNext: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Email Input Field
        androidx.compose.material3.OutlinedTextField(
            value = googleEmail,
            onValueChange = onEmailChange,
            label = { Text("Email or phone") },
            isError = googleEmailError.isNotEmpty(),
            supportingText = if (googleEmailError.isNotEmpty()) {
                { Text(googleEmailError, color = Color(0xFFB3261E)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0B57D0),
                focusedLabelColor = Color(0xFF0B57D0),
                unfocusedBorderColor = Color(0xFF747775),
                errorBorderColor = Color(0xFFB3261E),
                errorLabelColor = Color(0xFFB3261E)
            ),
            shape = RoundedCornerShape(4.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNext() })
        )

        // Forgot Email clickable
        Text(
            text = "Forgot email?",
            color = Color(0xFF0B57D0),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { /* display message or do nothing */ }
                .padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Guest mode disclaimer
        Text(
            text = "Not your computer? Use Guest mode to sign in privately. Learn more",
            fontSize = 12.sp,
            color = Color(0xFF5F6368),
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextButton(
                onClick = { /* mock signup click */ },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF0B57D0)
                )
            ) {
                Text("Create account", fontWeight = FontWeight.SemiBold)
            }

            androidx.compose.material3.Button(
                onClick = onNext,
                shape = RoundedCornerShape(100.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0B57D0)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Next", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun GooglePasswordFormSection(
    googlePassword: String,
    onPasswordChange: (String) -> Unit,
    googlePasswordError: String,
    showGooglePassword: Boolean,
    onShowPasswordToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Password Input
        androidx.compose.material3.OutlinedTextField(
            value = googlePassword,
            onValueChange = onPasswordChange,
            label = { Text("Enter your password") },
            isError = googlePasswordError.isNotEmpty(),
            supportingText = if (googlePasswordError.isNotEmpty()) {
                { Text(googlePasswordError, color = Color(0xFFB3261E)) }
            } else null,
            visualTransformation = if (showGooglePassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0B57D0),
                focusedLabelColor = Color(0xFF0B57D0),
                unfocusedBorderColor = Color(0xFF747775),
                errorBorderColor = Color(0xFFB3261E),
                errorLabelColor = Color(0xFFB3261E)
            ),
            shape = RoundedCornerShape(4.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onNext() })
        )

        // Show password checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowPasswordToggle(!showGooglePassword) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Checkbox(
                checked = showGooglePassword,
                onCheckedChange = onShowPasswordToggle,
                colors = androidx.compose.material3.CheckboxDefaults.colors(
                    checkedColor = Color(0xFF0B57D0)
                )
            )
            Text(
                text = "Show password",
                fontSize = 14.sp,
                color = Color(0xFF1F1F1F)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextButton(
                onClick = onBack,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF0B57D0)
                )
            ) {
                Text("Back", fontWeight = FontWeight.SemiBold)
            }

            androidx.compose.material3.Button(
                onClick = onNext,
                shape = RoundedCornerShape(100.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0B57D0)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Next", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
