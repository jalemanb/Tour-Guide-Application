package com.robotemi.sdk.sample

data class TemiCommand(var command: Int? = null,
                       var text:String? = null,
                       var x:Float? = null,
                       var y:Float? = null,
                       var angle:Float? = null,
                       var isBlocking:Boolean? = null){}