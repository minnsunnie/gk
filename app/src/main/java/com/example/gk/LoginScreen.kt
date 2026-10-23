package com.example.gk

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(
    onLoginSuccess: (isAdmin: Boolean) -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ECOMER LOGIN",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Ô NHẬP EMAIL
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Ô NHẬP MẬT KHẨU
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // NÚT ĐĂNG NHẬP
        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập Email và Mật khẩu!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true

                // 1. XÁC THỰC VỚI FIREBASE AUTHENTICATION
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val currentUser = authResult.user
                        if (currentUser != null) {
                            // Phân quyền theo email (chứa "admin" -> admin, ngược lại -> user)
                            val role = if (email.contains("admin", ignoreCase = true)) "admin" else "user"

                            // Đóng gói đầy đủ thông tin bao gồm cả PASSWORD
                            val userData = hashMapOf(
                                "uid" to currentUser.uid,
                                "email" to currentUser.email,
                                "password" to password,
                                "role" to role
                            )

                            // 2. LƯU/CẬP NHẬT THÔNG TIN VÀO COLLECTION "users" TRÊN FIRESTORE
                            db.collection("users").document(currentUser.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    isLoading = false
                                    Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(role == "admin")
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    // Trường hợp lưu Firestore gặp lỗi vẫn cho đăng nhập vào app
                                    onLoginSuccess(role == "admin")
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        isLoading = false
                        Toast.makeText(context, "Lỗi đăng nhập: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(text = "ĐĂNG NHẬP", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}