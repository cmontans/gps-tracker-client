package com.tracker.gps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tracker.gps.model.UserData

class UsersAdapter : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    private var users = listOf<UserData>()

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserSpeed: TextView = view.findViewById(R.id.tvUserSpeed)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvUserName.text = user.userName
        holder.tvUserSpeed.text = String.format("%.1f km/h", user.speed)
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<UserData>) {
        val diffResult = DiffUtil.calculateDiff(UsersDiffCallback(users, newUsers))
        users = newUsers
        diffResult.dispatchUpdatesTo(this)
    }

    private class UsersDiffCallback(
        private val oldList: List<UserData>,
        private val newList: List<UserData>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].userId == newList[newItemPosition].userId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.userName == new.userName && old.speed == new.speed
        }
    }
}
