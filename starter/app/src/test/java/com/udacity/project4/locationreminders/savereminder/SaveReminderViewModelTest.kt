package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.getOrAwaitValue


import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class SaveReminderViewModelTest {


    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var repository: ReminderDataSource
    private lateinit var app: Application

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup () {
        app = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testValidateDataEmptyTitleShouldReturnFalse() {
        repository = FakeDataSource()
        viewModel = SaveReminderViewModel(app, repository)

        val reminderNoTitle = ReminderDataItem(
            title = null,
            description = "description",
            latitude = 0.1,
            longitude = 0.1,
            location = "testlocation"
        )
        val result = viewModel.validateEnteredData(reminderNoTitle)
        assert(result == false)
    }

    @Test
    fun testValidateDataEmptyLocationShouldReturnFalse() {
        repository = FakeDataSource()
        app = ApplicationProvider.getApplicationContext()
        viewModel = SaveReminderViewModel(app, repository)

        val reminderNoLocation = ReminderDataItem(
            title = "title",
            description = "description",
            latitude = 0.1,
            longitude = 0.1,
            location = null
        )
        val result = viewModel.validateEnteredData(reminderNoLocation)
        assert(result == false)
    }

    @Test
    fun testValidateDataWithLocationTitleShouldReturnTrue() {
        repository = FakeDataSource()
        app = ApplicationProvider.getApplicationContext()
        viewModel = SaveReminderViewModel(app, repository)

        val reminderValid = ReminderDataItem(
            title = "title",
            description = "description",
            latitude = 0.1,
            longitude = 0.1,
            location = "testlocation"
        )
        val result = viewModel.validateEnteredData(reminderValid)
        assert(result == true)
    }

    @Test
    fun testSaveReminder() {

        repository = FakeDataSource()
        app = ApplicationProvider.getApplicationContext()
        viewModel = SaveReminderViewModel(app, repository)

        val reminderValid = ReminderDataItem(
            title = "title",
            description = "description",
            latitude = 0.1,
            longitude = 0.1,
            location = "testlocation"
        )

        runBlockingTest {

            mainCoroutineRule.pauseDispatcher()
            viewModel.saveReminder(reminderValid)
            val showLoadingValue = viewModel.showLoading.getOrAwaitValue()
            assert(showLoadingValue == true)

            mainCoroutineRule.resumeDispatcher()

            //test that reminder saved to repository
            val result = repository.getReminder(reminderValid.id)
            assert(result is Result.Success)
        }
        //test livedata variables after running save method
        val showToastValue = viewModel.showToast.getOrAwaitValue()
        val navigationCommandValue = viewModel.navigationCommand.getOrAwaitValue()
        val showLoadingValue = viewModel.showLoading.getOrAwaitValue()

        assert(showLoadingValue == false)
        assert(showToastValue == app.getString(R.string.reminder_saved))
        assert(
            navigationCommandValue == NavigationCommand.To(
                SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment()
            )
        )

    }
}


