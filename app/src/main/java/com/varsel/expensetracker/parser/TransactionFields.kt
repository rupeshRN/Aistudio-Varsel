package com.varsel.expensetracker.parser

data class TransactionFields(

    val ifsc: String? = null,

    val account: String? = null,

    val upiId: String? = null,

    val reference: String? = null,

    val channel: String? = null,

    val merchant: String? = null,

    val purpose: String? = null,

    val unknown: List<String> = emptyList(),

    val confidence: ConfidenceReport? = null
)
