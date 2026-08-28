package com.varsel.expensetracker.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.ui.transaction.components.BottomActionBar
import com.varsel.expensetracker.ui.transaction.components.CategorySection
import com.varsel.expensetracker.ui.transaction.components.DescriptionSection
import com.varsel.expensetracker.ui.transaction.components.TransactionInfoSection
import com.varsel.expensetracker.ui.transaction.components.TransactionLinkSection
import com.varsel.expensetracker.ui.transaction.components.TransferLinkSection
import com.varsel.expensetracker.domain.model.TransactionRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(

    transactionId:
        Long,

    viewModel:
        TransactionDetailViewModel,

    onBackClick:
        () -> Unit,

    onFinancialEventClick:
        (String) -> Unit

) {

    val uiState by
        viewModel.uiState
            .collectAsStateWithLifecycle()

    val saveCompleted by
        viewModel.saveCompleted
            .collectAsStateWithLifecycle()

    val scrollState =
        rememberScrollState()

    //--------------------------------------------------
    // Load transaction
    //--------------------------------------------------

    LaunchedEffect(transactionId) {

        viewModel.loadTransaction(
            transactionId
        )
    }

    //--------------------------------------------------
    // Handle successful save
    //--------------------------------------------------

    LaunchedEffect(saveCompleted) {

        if (
            saveCompleted
        ) {

            viewModel.consumeSaveCompleted()

            onBackClick()
        }
    }

    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = {

                    Text(
                        "Transaction Details"
                    )
                },

                navigationIcon = {

                    IconButton(

                        onClick =
                            onBackClick
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                }
            )
        },

        bottomBar = {

            val state =
                uiState as?
                    TransactionDetailUiState.Loaded

            if (
                state != null
            ) {

                BottomActionBar(

                    onDeleteClick = {

                        // E2.4
                    },

                    onSaveClick =
                        viewModel::saveChanges,

                    saveEnabled =
                        state.hasChanges &&
                        !state.isSaving
                )
            }
        }

    ) { padding ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(
                        scrollState
                    )
                    .padding(24.dp),

            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            when (
                val state =
                    uiState
            ) {

                //--------------------------------------------------
                // Loading
                //--------------------------------------------------

                TransactionDetailUiState.Loading -> {

                    Text(
                        "Loading..."
                    )
                }

                //--------------------------------------------------
                // Error
                //--------------------------------------------------

                is TransactionDetailUiState.Error -> {

                    Text(
                        state.message
                    )
                }

                //--------------------------------------------------
                // Loaded
                //--------------------------------------------------

                is TransactionDetailUiState.Loaded -> {

                    val transaction =
                        state.transaction

                    //--------------------------------------------------
                    // Description
                    //--------------------------------------------------

                    DescriptionSection(

                        description =
                            state
                                .editableDescription,

                        onDescriptionChanged =
                            viewModel::updateDescription
                    )

                    //--------------------------------------------------
                    // Category
                    //--------------------------------------------------

                    CategorySection(

                        selectedCategory =
                            state
                                .selectedCategory,

                        onCategorySelected =
                            viewModel::updateCategory
                    )

                    //--------------------------------------------------
                    // Transaction Role
                    //--------------------------------------------------

                    TransactionRoleSection(

                        transactionType =
                            transaction.type,

                        selectedRole =
                            state
                                .selectedRole,

                        onRoleSelected =
                            viewModel::updateRole
                    )

//--------------------------------------------------
// Financial Event
//
// Financial Events are NOT applicable to transfers.
//--------------------------------------------------

if (
    state.selectedRole !=
        TransactionRole.TRANSFER_IN &&

    state.selectedRole !=
        TransactionRole.TRANSFER_OUT
) {

    TransactionLinkSection(
        allocations = state.allocations,
        totalAllocatedAmount = state.totalAllocatedAmount,
        remainingUnallocatedAmount = state.remainingUnallocatedAmount,
        totalTransactionAmount = kotlin.math.abs(transaction.amount),
        allAvailableEventGroups = state.allAvailableEventGroups,
        showCreateGroupPrompt = state.showCreateGroupPrompt,
        showAllocateExistingPrompt = state.showAllocateExistingPrompt,
        editingAllocation = state.editingAllocation,
        allocationErrorMessage = state.allocationErrorMessage,
        isSavingGroup = state.isSavingGroup,
        categories = state.categories,
        onManageFinancialEvent = onFinancialEventClick,
        onShowCreateFinancialEvent = viewModel::showCreateGroupPrompt,
        onDismissCreateGroupPrompt = viewModel::dismissCreateGroupPrompt,
        onCreateReportGroup = { groupName, category, amount ->
            viewModel.createReportGroup(groupName, category, amount)
        },
        onShowAllocateExisting = viewModel::showAllocateExistingPrompt,
        onDismissAllocateExisting = viewModel::dismissAllocateExistingPrompt,
        onAllocateToExistingGroup = viewModel::allocateToExistingGroup,
        onStartEditingAllocation = viewModel::startEditingAllocation,
        onDismissEditingAllocation = viewModel::dismissEditingAllocation,
        onUpdateAllocationAmount = viewModel::updateAllocationAmount,
        onDeleteAllocation = viewModel::deleteAllocation,
        onClearError = viewModel::clearAllocationError
    )
}

//--------------------------------------------------
// Transfer In / Transfer Out
//
// Transfers have their own relationship and are
// intentionally kept separate from Financial Events.
//
// TRANSFER_OUT:
//     show possible TRANSFER_IN transactions.
//
// TRANSFER_IN:
//     show possible TRANSFER_OUT transactions.
//
// Normal transactions:
//     no transfer section.
//--------------------------------------------------

if (
    state.selectedRole ==
        TransactionRole.TRANSFER_IN ||

    state.selectedRole ==
        TransactionRole.TRANSFER_OUT
) {

    TransferLinkSection(

        transaction =
            transaction,

        linkedTransfer =
            state.linkedTransfer,

        candidateTransactions =
            state.transferCandidates,

        isLinking =
            state.isTransferLinking,

        transferErrorMessage =
            state.transferErrorMessage,

        onLinkTransfer = { candidateId ->

            viewModel.linkTransfer(
                candidateId
            )
        },

        onUnlinkTransfer = {

            viewModel.unlinkTransfer()
        },

        onClearError = {

            viewModel.clearTransferError()
        }
    )
}

                    //--------------------------------------------------
                    // Transaction Information
                    //--------------------------------------------------

                    TransactionInfoSection(

                        amount =
                            "₹%.2f"
                                .format(
                                    transaction.amount
                                ),

                        date =
                            SimpleDateFormat(

                                "dd MMM yyyy",

                                Locale.ENGLISH

                            ).format(

                                Date(
                                    transaction.dateTimestamp
                                )
                            ),

                        type =
                            transaction.type.name
                    )

                    //--------------------------------------------------
                    // Bottom spacing
                    //--------------------------------------------------

                    Spacer(

                        modifier =
                            Modifier.padding(
                                bottom = 24.dp
                            )
                    )
                }
            }
        }
    }
}
