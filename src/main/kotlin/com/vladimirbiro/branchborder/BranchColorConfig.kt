package com.vladimirbiro.branchborder

import com.google.gson.annotations.SerializedName

data class BlinkingConfig(
    val enabled: Boolean = false,
    val interval: Int = 500
)

data class BranchRule(
    val pattern: String,
    val match: String = "exact",
    val color: String,
    val blinking: BlinkingConfig? = null
)

data class BranchColorConfigData(
    val borderWidth: Int = 4,
    val defaultColor: String? = null,
    val blinking: BlinkingConfig? = null,
    @SerializedName("branches")
    val branches: List<BranchRule> = emptyList()
)
