package com.example

import org.junit.Assert.*
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request

class ExampleUnitTest {
  @Test
  fun testSupabaseGet() {
    val client = OkHttpClient()
    val request = Request.Builder()
      .url("https://tmfoduaorgidxnoeqkfs.supabase.co/rest/v1/Apis")
      .header("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRtZm9kdWFvcmdpZHhub2Vxa2ZzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODEwNDA1MjgsImV4cCI6MjA5NjYxNjUyOH0.Oh_-QWCtniH_Iu7st_lULznIjLRdpz3T5Dc63zXfDW0")
      .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRtZm9kdWFvcmdpZHhub2Vxa2ZzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODEwNDA1MjgsImV4cCI6MjA5NjYxNjUyOH0.Oh_-QWCtniH_Iu7st_lULznIjLRdpz3T5Dc63zXfDW0")
      .build()
    
    val response = client.newCall(request).execute()
    val body = response.body?.string()
    println("SUPABASE_RESPONSE_BODY: " + body)
    assertNotNull(body)
  }
}
