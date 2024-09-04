package com.airei.milltracking.mypalm.mqtt.lrc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.roomdb.DBRepository
import com.airei.milltracking.mypalm.mqtt.lrc.roomdb.DoorTable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val doorRepository: DBRepository
):ViewModel()
{

    val startMqtt = MutableLiveData<Boolean>(false)

    val updateDoor = MutableLiveData<String>()
    val updateStarter = MutableLiveData<String>()

    // LiveData or other observables for the UI
    val doorsLiveData = doorRepository.getAllDoors()

    fun insertAllDoors(doors: List<DoorTable>) {
        viewModelScope.launch {
            doorRepository.insertAll(doors)
        }
    }
    fun updateAllDoors(doors: List<DoorTable>) {
        viewModelScope.launch {
            doorRepository.updateList(doors)
        }
    }

    fun deleteDoor(door: DoorTable) {
        viewModelScope.launch {
            doorRepository.deleteDoor(door)
        }
    }
}