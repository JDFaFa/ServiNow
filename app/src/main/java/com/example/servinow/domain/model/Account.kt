package com.example.servinow.domain.model
data class AccountData(val name:String="",val email:String="",val phone:String="",val photo:String="",val lat:Double?=null,val lng:Double?=null,val title:String="",val about:String="",val years:String="",val specialty:String="Plomería",val cv:String="",val worker:Boolean=false)
data class Notice(val id:String,val text:String,val target:String,val post:String="",val read:Boolean=false)
