package com.tracker.gps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tracker.gps.model.UserData

class UsersAdapter : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    private var users = listOf<UserData>()
    private var speedUnit: String = "kmh"

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
        
        val convertedSpeed = when (speedUnit) {
            "mph" -> user.speed * com.tracker.gps.shared.util.Constants.KMH_TO_MPH
            "knots" -> user.speed * com.tracker.gps.shared.util.Constants.KMH_TO_KNOTS
            else -> user.speed
        }
        val unitLabel = when (speedUnit) {
            "mph" -> "mph"
            "knots" -> "knots"
            else -> "km/h"
        }
        holder.tvUserSpeed.text = String.format("%.1f %s", convertedSpeed, unitLabel)
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<UserData>) {
        users = newUsers
        notifyDataSetChanged()
    }

    fun setSpeedUnit(unit: String) {
        speedUnit = unit
        notifyDataSetChanged()
    }
}
