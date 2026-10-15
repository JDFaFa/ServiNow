package com.example.servinow.features.account
import com.example.servinow.domain.model.AccountData
import com.example.servinow.domain.model.Publication

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.servinow.domain.repository.AccountRepository
@HiltViewModel
class AccountViewModel @Inject constructor(private val repository:AccountRepository):ViewModel() {
 val data=repository.data
 val notices=repository.notices
 val points=repository.points
 fun save(a:AccountData)=repository.save(a)
 fun mark(id:String,read:Boolean)=repository.mark(id,read)
 fun markAll()=repository.markAll()
 fun sync(posts:List<Publication>,comments:Int)=repository.sync(posts,comments)
 fun achieved(key:String)=repository.achieved(key)
}
