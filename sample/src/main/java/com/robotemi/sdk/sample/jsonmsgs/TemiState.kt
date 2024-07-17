package com.robotemi.sdk.sample.jsonmsgs

data class TemiState(var timestamp:String? = null,
                     var soc:Int? = null,
                     var isCharging:Boolean? = null,
                     var action:String? = null)