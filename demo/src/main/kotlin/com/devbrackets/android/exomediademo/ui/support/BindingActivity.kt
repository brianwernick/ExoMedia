package com.devbrackets.android.exomediademo.ui.support

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * An Activity base class that handles the setup of boilerplate such as the ViewBindings
 */
abstract class BindingActivity<BINDING : ViewBinding> : AppCompatActivity() {
    private var backerBinding: BINDING? = null

    /**
     * The Binding that has been inflated.
     * This is only available between `onCreateView` and `onDestroyView`, otherwise will
     * function in a similar manner to `requireContext()` by throwing an `IllegalStateException`
     */
    protected val binding: BINDING
        get() = backerBinding ?: throw IllegalStateException("Binding not attached to Activity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backerBinding = inflateBinding(layoutInflater)
        onBindingCreated(binding)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        backerBinding = null
    }

    abstract fun inflateBinding(layoutInflater: LayoutInflater): BINDING

    open fun onBindingCreated(binding: BINDING) {
        // Purposefully left blank
    }
}