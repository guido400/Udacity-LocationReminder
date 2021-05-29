package com.udacity.project4.locationreminders.reminderslist


import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [29])
class RemindersListViewModelTest {

    private lateinit var viewModel:RemindersListViewModel
    private lateinit var repository:FakeDataSource
    private lateinit var app:Application

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
    fun testSuccessResultShouldBeSavedToRemindersList(){

        repository = FakeDataSource()
        viewModel = RemindersListViewModel(app,repository)

        val reminderDTO1 = ReminderDTO("test_title1",
            "test_description",
            "testlocation",
            0.1,
            0.1)
        val reminderDTO2 = ReminderDTO("test_title2",
            "test_description",
            "testlocation",
            0.1,
            0.1)


        runBlockingTest {
            repository.saveReminder(reminderDTO1)
            repository.saveReminder(reminderDTO2)
        }

        viewModel.loadReminders()

        val value = viewModel.remindersList.getOrAwaitValue()

        assert(value[0].id == reminderDTO1.id)
        assert(value[1].id == reminderDTO2.id)
        assert(value.size == 2)
    }

    @Test
    fun testErrorEmptyResultShouldSetNoDataValueTrue() {

        repository = FakeDataSource()
        viewModel = RemindersListViewModel(app,repository)

        viewModel.loadReminders()
        val value = viewModel.showNoData.getOrAwaitValue()

        assert (value == true)
    }

    @Test
    fun testErrorResultShouldSetSnackBarLiveData(){
        repository = FakeDataSource()
        viewModel = RemindersListViewModel(app,repository)

        repository.setReturnError(true)
        viewModel.loadReminders()

        val value = viewModel.showSnackBar.getOrAwaitValue()
        assert(value == "Test exception")
    }

}