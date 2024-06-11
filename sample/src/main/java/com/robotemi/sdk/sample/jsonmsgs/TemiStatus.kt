package com.robotemi.sdk.sample.jsonmsgs

data class TemiStatus(var command:Int? = null,
                      var status:String? = null,
                      var output:MutableList<String>? = null)
