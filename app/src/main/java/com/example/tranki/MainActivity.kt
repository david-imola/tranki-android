package com.example.tranki

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcel
import android.view.Menu
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class PagerAdapter(fm: FragmentManager, lc: Lifecycle) : FragmentStateAdapter(fm, lc) {

    var fragments = ArrayList<TranslateFragment>()
    fun addFragment() {
        fragments.add(TranslateFragment.newInstance(fragments.size))
        notifyDataSetChanged()
    }

    fun removeFragment(position: Int) {
        fragments.removeAt(position)
        for (i in 0 until fragments.size) {
            val f = fragments[i] as TranslateFragment
            f.setTabPosition(i)
            fragments[i] = f
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    override fun getItemId(position: Int): Long {
        return fragments[position].hashCode().toLong() // make sure notifyDataSetChanged() works
    }

    override fun containsItem(itemId: Long): Boolean {
        return fragments.any { it.hashCode().toLong() == itemId }
    }

}

class MainActivity : AppCompatActivity() {

    private lateinit var pagerAdapter: PagerAdapter
    private lateinit var tabLayout: TabLayout
    private var tabTexts = ArrayList<String>()

    private val sharedPrefsKey = "mainSharedPreferences"

    private val divider = "->"

    private val onMenuItemClickListener = Toolbar.OnMenuItemClickListener { menuItem ->
        when (menuItem?.itemId) {
            R.id.action_setup -> {
                startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                true
            }

            R.id.action_settings -> {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                true
            }

            else -> false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.inflateMenu(R.menu.menu_main)
        toolbar.setOnMenuItemClickListener(onMenuItemClickListener)

        tabLayout = findViewById(R.id.tab_layout)
        val viewPager: ViewPager2 = findViewById(R.id.pager)
        pagerAdapter = PagerAdapter(supportFragmentManager, lifecycle)


        savedInstanceState?.let {
            tabTexts = it.getStringArrayList("tabTexts") ?: ArrayList<String>()
            if (it.containsKey("fragments")) {
                pagerAdapter.fragments =
                    it.getParcelableArrayList("fragments", TranslateFragment::class.java)!!
                pagerAdapter.notifyDataSetChanged()
            }
        }

        if (savedInstanceState == null) {
            val sharedPreferences = getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE)
            sharedPreferences.getString("tabTexts", null)?.let {
                val gson = Gson()
                val type = object : TypeToken<ArrayList<String>>() {}.type
                tabTexts = gson.fromJson(it, type)
            }
            sharedPreferences.getString("fragments", null)?.let {
                val bytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                val parcel = Parcel.obtain()
                parcel.unmarshall(bytes, 0, bytes.size)
                parcel.setDataPosition(0)
                pagerAdapter.fragments = parcel.readArrayList(
                    TranslateFragment::class.java.classLoader,
                    TranslateFragment::class.java
                ) as ArrayList<TranslateFragment>
                parcel.recycle()
            }
        }


        viewPager.adapter = pagerAdapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTexts[position]
        }.attach()

        val plusButton: ImageButton = findViewById(R.id.btn_add_tab)
        plusButton.setOnClickListener {
            tabTexts.add("af->af")
            pagerAdapter.addFragment()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    fun removeFragment(position: Int) {
        tabTexts.removeAt(position) // this must go before removeFragment
        pagerAdapter.removeFragment(position)
    }

    fun setTabLeftText(position: Int, left: String) {
        if (position < tabTexts.size) {
            val text = tabTexts[position]
            val right = text.split(divider)[1]
            val tabText = "$left$divider$right"
            tabLayout.getTabAt(position)!!.text = tabText
            tabTexts[position] = tabText
        }
    }

    fun setTabRightText(position: Int, right: String) {
        if (position < tabTexts.size) {
            val text = tabTexts[position]
            val left = text.split(divider)[0]
            val tabText = "$left$divider$right"
            tabLayout.getTabAt(position)!!.text = tabText
            tabTexts[position] = tabText
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("fragments", pagerAdapter.fragments)
        outState.putStringArrayList("tabTexts", tabTexts)
    }

    override fun onStop() {
        super.onStop()



        // only write those fragments whose tab index is > -1
        var i = 0
        for(frag in pagerAdapter.fragments) {
            if (frag.getTabPosition() == -1)
                break
            i++
        }

        // fragments
        val fragmentsSubList = pagerAdapter.fragments.subList(0, i)
        val parcel = Parcel.obtain()
        parcel.writeList(fragmentsSubList)
        val bytes = parcel.marshall()
        parcel.recycle()
        val fragments = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)

        // tab texts
        val tabTextsSubList = tabTexts.subList(0, i)
        val gson = Gson()
        val tabTexts = gson.toJson(tabTextsSubList)

        val sharedPreferences = getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("fragments", fragments)
        editor.putString("tabTexts", tabTexts)
        editor.apply()

    }

}