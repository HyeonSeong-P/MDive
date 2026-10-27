package com.parka.mdive.testing

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/** Empty debug-only host; prepared screen state is supplied by instrumentation tests. */
@AndroidEntryPoint
class ProductTestActivity : ComponentActivity()
