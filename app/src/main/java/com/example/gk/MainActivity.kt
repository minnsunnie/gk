package com.example.gk

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = FirebaseAuth.getInstance()
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var isAdmin by remember { mutableStateOf(auth.currentUser?.email?.contains("admin", true) == true) }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { adminStatus ->
                isAdmin = adminStatus
                isLoggedIn = true
            }
        )
    } else {
        ProductMainScreen(
            isAdmin = isAdmin,
            onLogout = {
                auth.signOut()
                isLoggedIn = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductMainScreen(
    isAdmin: Boolean,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var tenSp by remember { mutableStateOf("") }
    var loaiSp by remember { mutableStateOf("") }
    var giaSp by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    val productList = remember { mutableStateListOf<Product>() }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) {
        db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Lỗi đọc dữ liệu!", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                productList.clear()
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val product = doc.toObject(Product::class.java)
                        if (product != null) {
                            product.id = doc.id
                            productList.add(product)
                        }
                    }
                }
            }
    }

    fun clearInputs() {
        tenSp = ""
        loaiSp = ""
        giaSp = ""
        imageUrl = ""
        selectedProduct = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isAdmin) "ECOMER (ADMIN)" else "ECOMER (USER)",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Red)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            if (isAdmin) {
                OutlinedTextField(
                    value = tenSp,
                    onValueChange = { tenSp = it },
                    label = { Text("Tên sản phẩm") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = loaiSp,
                    onValueChange = { loaiSp = it },
                    label = { Text("Loại sản phẩm") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = giaSp,
                    onValueChange = { giaSp = it },
                    label = { Text("Giá") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL Hình ảnh (Image)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))


                Button(
                    onClick = {
                        if (tenSp.isEmpty() || loaiSp.isEmpty() || giaSp.isEmpty()) {
                            Toast.makeText(context, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val gia = giaSp.toLongOrNull() ?: 0L

                        if (selectedProduct == null) {
                            val newProduct = hashMapOf(
                                "tenSp" to tenSp,
                                "loaiSp" to loaiSp,
                                "gia" to gia,
                                "imageUrl" to imageUrl
                            )
                            db.collection("products").add(newProduct)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Thêm thành công!", Toast.LENGTH_SHORT).show()
                                    clearInputs()
                                }
                        } else {
                            val updateData = mapOf(
                                "tenSp" to tenSp,
                                "loaiSp" to loaiSp,
                                "gia" to gia,
                                "imageUrl" to imageUrl
                            )
                            db.collection("products").document(selectedProduct!!.id).update(updateData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                    clearInputs()
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text(
                        text = if (selectedProduct == null) "THÊM SẢN PHẨM" else "CẬP NHẬT SẢN PHẨM",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Danh sách sản phẩm:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(productList) { product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(product.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(70.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = android.R.drawable.stat_notify_error),
                            placeholder = painterResource(id = android.R.drawable.ic_menu_gallery)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Tên sp: ${product.tenSp}", fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(text = "Giá sp: ${product.gia}", color = Color.DarkGray)
                            Text(text = "Loại sp: ${product.loaiSp}", color = Color.Gray)
                        }


                        if (isAdmin) {
                            IconButton(onClick = {
                                selectedProduct = product
                                tenSp = product.tenSp
                                loaiSp = product.loaiSp
                                giaSp = product.gia.toString()
                                imageUrl = product.imageUrl
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFFBC02D))
                            }

                            IconButton(onClick = {
                                db.collection("products").document(product.id).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Đã xóa sản phẩm!", Toast.LENGTH_SHORT).show()
                                    }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}