package com.example.tranki

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcel
import android.view.Menu
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
import com.google.mlkit.nl.translate.TranslateLanguage


class PagerAdapter(fm: FragmentManager, lc: Lifecycle) : FragmentStateAdapter(fm, lc) {

    var fragments = ArrayList<TranslateFragment>()

    fun initFragments(lang1: String, lang2: String) {
        fragments.add(TranslateFragment.newInstance(0,
            lang1, lang2))
        fragments.add(TranslateFragment.newInstance(1,
            lang2, lang1))
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

    private val LANG1 = TranslateLanguage.ITALIAN
    private val LANG2 = TranslateLanguage.ENGLISH

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

//        tabLayout.getTabAt(0)!!.text = "{LANG1}->{LANG2}"
//        tabLayout.getTabAt(1)!!.text = "{LANG2}->{LANG1}"


        savedInstanceState?.let {
            tabTexts = it.getStringArrayList("tabTexts") ?: ArrayList<String>()
            if (it.containsKey("fragments")) {
                pagerAdapter.fragments =
                    it.getParcelableArrayList("fragments", TranslateFragment::class.java)!!
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

        tabTexts.add("${LANG1}->${LANG2}")
        tabTexts.add("${LANG2}->${LANG1}")

        if (pagerAdapter.fragments.isEmpty()) {
            pagerAdapter.initFragments(LANG1, LANG2)
        }
        pagerAdapter.notifyDataSetChanged()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }



    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("fragments", pagerAdapter.fragments)
        outState.putStringArrayList("tabTexts", tabTexts)
    }

    override fun onStop() {
        super.onStop()




        // fragments
        val fragmentsSubList = pagerAdapter.fragments
        val parcel = Parcel.obtain()
        parcel.writeList(fragmentsSubList)
        val bytes = parcel.marshall()
        parcel.recycle()
        val fragments = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)

        // tab texts
        val tabTextsSubList = tabTexts
        val gson = Gson()
        val tabTexts = gson.toJson(tabTextsSubList)

        val sharedPreferences = getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("fragments", fragments)
        editor.putString("tabTexts", tabTexts)
        editor.apply()

    }

}