package com.varsel.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BankLogoBadge(
    bankName: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val normalized = bankName.uppercase()

    when {
        normalized.contains("HDFC") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF004C8F)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size * 0.75f)
                            .border(1.5.dp, Color(0xFFED1C24), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HDFC",
                            color = Color.White,
                            fontSize = (size.value * 0.22f).sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
        normalized.contains("SBI") || normalized.contains("STATE BANK") -> {
            Surface(
                modifier = modifier.size(size),
                shape = CircleShape,
                color = Color(0xFF1E5BB5)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // SBI iconic circular emblem with keyhole slot
                    Box(
                        modifier = Modifier
                            .size(size * 0.5f)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .size(size * 0.22f, size * 0.28f)
                                .background(Color(0xFF1E5BB5))
                        )
                    }
                }
            }
        }
        normalized.contains("ICICI") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFB32729)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(size * 0.28f)
                                .clip(CircleShape)
                                .background(Color(0xFFF37021))
                        )
                        Text(
                            text = "i",
                            color = Color.White,
                            fontSize = (size.value * 0.45f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }
        }
        normalized.contains("AXIS") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF97144D)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▲",
                        color = Color(0xFFED1C24),
                        fontSize = (size.value * 0.35f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        normalized.contains("KOTAK") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFEE1C25)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "∞",
                        color = Color.White,
                        fontSize = (size.value * 0.48f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        normalized.contains("BARODA") || normalized.contains("BOB") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF26522)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BOB",
                        color = Color.White,
                        fontSize = (size.value * 0.25f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        normalized.contains("CANARA") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0084C9)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▲",
                        color = Color(0xFFFFD200),
                        fontSize = (size.value * 0.35f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        normalized.contains("PNB") || normalized.contains("PUNJAB") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFA20B27)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PNB",
                        color = Color(0xFFFFC20E),
                        fontSize = (size.value * 0.24f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        normalized.contains("IDFC") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF9B1C26)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "IDFC",
                        color = Color.White,
                        fontSize = (size.value * 0.24f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        normalized.contains("FEDERAL") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF003E7E)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FED",
                        color = Color(0xFFFFC72C),
                        fontSize = (size.value * 0.25f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        normalized.contains("PAYTM") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF002E6E)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "paytm",
                        color = Color(0xFF00BAF2),
                        fontSize = (size.value * 0.22f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        else -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalance,
                        contentDescription = bankName,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }
        }
    }
}
