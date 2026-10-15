package com.example.servinow.domain.repository
import com.example.servinow.domain.model.*
import kotlinx.coroutines.flow.StateFlow
interface AccountRepository {
 val data:StateFlow<AccountData>
 val notices:StateFlow<List<Notice>>
 val points:StateFlow<Int>
 fun reload()
 fun save(a:AccountData):Boolean
 fun mark(id:String,read:Boolean)
 fun markAll()
 fun sync(posts:List<Publication>,comments:Int)
 fun achieved(key:String):Boolean
}
