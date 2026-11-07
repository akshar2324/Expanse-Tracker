package com.akshar.ui.viewmodel

import android.app.Application
import org.junit.Test

/**
 * The default AndroidViewModelFactory creates AndroidViewModels through a constructor that takes
 * only Application. A ViewModel that adds a parameter with a default value needs @JvmOverloads,
 * or it crashes the moment its screen opens.
 */
class ViewModelFactoryContractTest {

    @Test
    fun `CsvImportViewModel can be created by the default factory`() {
        CsvImportViewModel::class.java.getConstructor(Application::class.java)
    }

    @Test
    fun `FinanceViewModel can be created by the default factory`() {
        FinanceViewModel::class.java.getConstructor(Application::class.java)
    }
}
