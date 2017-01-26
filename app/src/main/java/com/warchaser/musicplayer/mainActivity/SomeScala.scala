package com.warchaser.musicplayer.mainActivity

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.warchaser.musicplayer.R

/**
 * Created by Wucn on 2017/1/21.
 */
class SomeScala extends Activity {
  var mTextView : TextView
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  override def onResume()
  {
    super.onResume()
  }

  private def initialize()
  {
    mTextView = findViewById(R.id.search_src_text).asInstanceOf[TextView];
  }
}
