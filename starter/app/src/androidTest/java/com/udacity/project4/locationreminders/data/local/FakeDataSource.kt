package com.udacity.project4.locationreminders.data.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.runBlocking

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    var tasksServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()
    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }
        return Result.Success(tasksServiceData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        tasksServiceData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }

        val reminder = tasksServiceData[id] ?: return Result.Error("Reminder not found!")
        return Result.Success(reminder)
    }

    override suspend fun deleteAllReminders() {
        tasksServiceData.clear()
    }


}