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
        // Indian Bank (Official Deep Blue + Golden/Yellow emblem)
        normalized.contains("INDIAN BANK") || normalized.contains("IDIB") || normalized.contains("IND BL") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF003B70)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFB800),
                            modifier = Modifier.size(size * 0.44f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "IB",
                                    color = Color(0xFF003B70),
                                    fontSize = (size.value * 0.20f).sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // HDFC Bank (Official Navy Blue + Red & White emblem)
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
                            .size(size * 0.72f)
                            .border(1.5.dp, Color(0xFFED1C24), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HDFC",
                            color = Color.White,
                            fontSize = (size.value * 0.20f).sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        // SBI (State Bank of India - Official Keyhole Blue circle)
        normalized.contains("SBI") || normalized.contains("STATE BANK") || normalized.contains("SBIN") -> {
            Surface(
                modifier = modifier.size(size),
                shape = CircleShape,
                color = Color(0xFF0066B3)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // SBI iconic circular keyhole emblem
                    Box(
                        modifier = Modifier
                            .size(size * 0.52f)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .size(size * 0.18f, size * 0.28f)
                                .background(Color(0xFF0066B3))
                        )
                    }
                }
            }
        }

        // ICICI Bank (Official Maroon & Orange Arc)
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
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
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
                            fontSize = (size.value * 0.44f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }
        }

        // Axis Bank (Official Burgundy + Inverted A-pyramid)
        normalized.contains("AXIS") || normalized.contains("UTIB") -> {
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
                        color = Color.White,
                        fontSize = (size.value * 0.38f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Kotak Mahindra Bank (Official Red + Infinity symbol)
        normalized.contains("KOTAK") || normalized.contains("KKBK") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFED1C24)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "∞",
                        color = Color.White,
                        fontSize = (size.value * 0.50f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bank of Baroda (Baroda Vermillion Sun)
        normalized.contains("BARODA") || normalized.contains("BOB") || normalized.contains("BARB") -> {
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
                        fontSize = (size.value * 0.24f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Canara Bank (Official Blue + Yellow interlocking triangles)
        normalized.contains("CANARA") || normalized.contains("CNRB") -> {
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
                        fontSize = (size.value * 0.38f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Punjab National Bank (PNB Maroon & Gold)
        normalized.contains("PNB") || normalized.contains("PUNJAB") || normalized.contains("PUNB") -> {
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

        // Union Bank of India (Union Red & Blue)
        normalized.contains("UNION") || normalized.contains("UBIN") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFD2232A)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "UBI",
                        color = Color.White,
                        fontSize = (size.value * 0.24f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // IDFC FIRST Bank (Official FIRST Maroon)
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
                        fontSize = (size.value * 0.23f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Indian Overseas Bank (IOB Blue & Gold)
        normalized.contains("IOB") || normalized.contains("IOBA") || normalized.contains("OVERSEAS") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF005A9C)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "IOB",
                        color = Color(0xFFFFD700),
                        fontSize = (size.value * 0.24f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Federal Bank (Federal Blue & Yellow)
        normalized.contains("FEDERAL") || normalized.contains("FDRL") -> {
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

        // IndusInd Bank (Indus Crimson)
        normalized.contains("INDUSIND") || normalized.contains("INDB") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF861F41)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "INDB",
                        color = Color.White,
                        fontSize = (size.value * 0.21f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Paytm Payments Bank
        normalized.contains("PAYTM") || normalized.contains("PYTM") -> {
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
                        fontSize = (size.value * 0.20f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Airtel Payments Bank
        normalized.contains("AIRTEL") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFED1C24)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "airtel",
                        color = Color.White,
                        fontSize = (size.value * 0.20f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Yes Bank
        normalized.contains("YES BANK") || normalized.contains("YESB") -> {
            Surface(
                modifier = modifier.size(size),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF005BA6)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "YES",
                        color = Color(0xFFED1C24),
                        fontSize = (size.value * 0.24f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Fallback Bank Badge
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
