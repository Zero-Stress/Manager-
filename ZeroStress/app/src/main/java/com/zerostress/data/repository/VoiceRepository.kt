package com.zerostress.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zerostress.data.model.VoiceChannel
import com.zerostress.data.model.VoiceParticipant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class VoiceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val channelsRef = db.collection("voiceChannels")

    // ==================== CHANNELS ====================

    fun listenChannels(): Flow<List<VoiceChannel>> = callbackFlow {
        val reg = channelsRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val channels = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(VoiceChannel::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(channels)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createChannel(name: String, type: String, createdBy: String): String {
        val docRef = channelsRef.document()
        val channel = VoiceChannel(
            id = docRef.id,
            name = name,
            type = type,
            createdBy = createdBy
        )
        docRef.set(channel).await()
        return docRef.id
    }

    suspend fun deleteChannel(channelId: String) {
        channelsRef.document(channelId).delete().await()
    }

    // ==================== JOIN / LEAVE ====================

    suspend fun joinChannel(channelId: String, participant: VoiceParticipant) {
        val docRef = channelsRef.document(channelId)
        docRef.update("participants", com.google.firebase.firestore.FieldValue.arrayUnion(participant)).await()
    }

    suspend fun leaveChannel(channelId: String, phone: String) {
        val doc = channelsRef.document(channelId).get().await()
        val channel = doc.toObject(VoiceChannel::class.java) ?: return
        val updated = channel.participants.filter { it.phone != phone }
        channelsRef.document(channelId).update("participants", updated).await()
    }

    // ==================== MUTE / DEAFEN ====================

    suspend fun toggleMute(channelId: String, phone: String, isMuted: Boolean) {
        val doc = channelsRef.document(channelId).get().await()
        val channel = doc.toObject(VoiceChannel::class.java) ?: return
        val updated = channel.participants.map {
            if (it.phone == phone) it.copy(isMuted = isMuted) else it
        }
        channelsRef.document(channelId).update("participants", updated).await()
    }

    suspend fun toggleDeafen(channelId: String, phone: String, isDeafened: Boolean) {
        val doc = channelsRef.document(channelId).get().await()
        val channel = doc.toObject(VoiceChannel::class.java) ?: return
        val updated = channel.participants.map {
            if (it.phone == phone) it.copy(isDeafened = isDeafened, isMuted = if (isDeafened) true else it.isMuted) else it
        }
        channelsRef.document(channelId).update("participants", updated).await()
    }

    suspend fun setSpeaking(channelId: String, phone: String, isSpeaking: Boolean) {
        val doc = channelsRef.document(channelId).get().await()
        val channel = doc.toObject(VoiceChannel::class.java) ?: return
        val updated = channel.participants.map {
            if (it.phone == phone) it.copy(isSpeaking = isSpeaking) else it
        }
        channelsRef.document(channelId).update("participants", updated).await()
    }

    // ==================== KICK ====================

    suspend fun kickParticipant(channelId: String, phone: String) {
        leaveChannel(channelId, phone)
    }
}
