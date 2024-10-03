package com.airei.milltracking.mypalm.mqtt.lrc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airei.milltracking.mypalm.mqtt.lrc.commons.CommandData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.FfbRunningStatus
import com.airei.milltracking.mypalm.mqtt.lrc.commons.SfbRunningStatus
import com.airei.milltracking.mypalm.mqtt.lrc.commons.StatusData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.statusDataSample
import com.airei.milltracking.mypalm.mqtt.lrc.commons.toStatusData
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

    var ffbLastStatus : MutableLiveData<FfbRunningStatus?> = MutableLiveData(null)
 var sfbLastStatus : MutableLiveData<SfbRunningStatus?> = MutableLiveData(null)

    val startMqtt = MutableLiveData<Boolean>(false)

    val statusData = MutableLiveData<StatusData?>(null)

    val commendData = MutableLiveData<CommandData>()

    val updateDoor = MutableLiveData<String>()
    val updateStarter = MutableLiveData<String>()
    val updateAiModeData = MutableLiveData<String>()

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