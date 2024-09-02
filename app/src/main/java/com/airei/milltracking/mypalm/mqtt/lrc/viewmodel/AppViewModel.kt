package com.airei.milltracking.mypalm.mqtt.lrc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor():ViewModel()
{

    val publishedMsg = MutableLiveData<Pair<Boolean,DoorData>>()

}