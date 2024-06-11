package com.robotemi.sdk.sample.jsonmsgs

data class TemiCommand(var command: Int? = null,
                       var text:String? = null,
                       var x:Float? = null,
                       var y:Float? = null,
                       var angle:Float? = null,
                       var flag0:Boolean = false,
                       var flag1:Boolean = false,
                       var flag2:Boolean = false,
                       var flag3:Boolean = false){}