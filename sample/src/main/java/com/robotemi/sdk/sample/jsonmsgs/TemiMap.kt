package com.robotemi.sdk.sample.jsonmsgs

data class TemiMap(var height:Int? = null,
                   var width:Int? = null,
                   var originX:Float? = null,
                   var originY:Float? = null,
                   var resolution:Float? = null,
                   var map:List<Int>,
                   var locations:List<Location>){}
