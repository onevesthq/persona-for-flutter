package com.jorgefspereira.persona_flutter

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import androidx.annotation.NonNull
import com.withpersona.sdk.inquiry.*

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/** PersonaFlutterPlugin */
public class PersonaFlutterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  private lateinit var channel : MethodChannel
  private var activity: Activity? = null
  private var binding: ActivityPluginBinding? = null
  private val requestCode = 57

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "persona_flutter")
      val plugin = PersonaFlutterPlugin()
      plugin.activity = registrar.activity()
      registrar.addActivityResultListener(plugin);
      channel.setMethodCallHandler(plugin)
    }
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "persona_flutter")
    channel.setMethodCallHandler(this);
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "start" -> {
        val activity = this.activity ?: return

        val inquiryId = call.argument<String>("inquiryId")
        val sessionToken = call.argument<String>("sessionToken")
        val templateId = call.argument<String>("templateId")
        val referenceId = call.argument<String>("referenceId")
        val accountId = call.argument<String>("accountId")
        val environment = call.argument<String>("environment")
        val fieldsMap = call.argument<Map<String, Any>>("fields")
        val note = call.argument<String>("note")

        if (inquiryId != null) {
          Inquiry.fromInquiry(inquiryId)
                  .sessionToken(sessionToken)
                  .build()
                  .start(activity, requestCode);

          result.success("Inquiry started with inquiryId")
        }
        else if(templateId != null) {

          val fieldsBuilder = Fields.Builder()

          if (fieldsMap != null) {

            val nameMap = fieldsMap["name"] as? Map<*, *>
            val addressMap = fieldsMap["address"] as? Map<*, *>
            val emailAddress = fieldsMap["emailAddress"] as? String
            val phoneNumber = fieldsMap["phoneNumber"] as? String
            val birthdate = fieldsMap["birthdate"] as? String
            val additionalFields = fieldsMap["additionalFields"] as? Map<String, *>

            if(nameMap != null) {
              val nameFirst = nameMap["first"] as String?
              val nameMiddle = nameMap["middle"] as String?
              val nameLast = nameMap["last"] as String?

              if (nameFirst != null){
                println(nameFirst);
                fieldsBuilder.field("nameFirst", nameFirst)
              }

              if (nameMiddle != null){
                println(nameMiddle)
                fieldsBuilder.field("nameMiddle", nameMiddle)
              }

              if (nameLast != null){
                println(nameLast)
                fieldsBuilder.field("nameLast", nameLast)
              }
            }

            if(addressMap != null ) {
              val addressStreet1 = addressMap["street1"] as? String
              val addressStreet2 = addressMap["street2"] as? String
              val addressCity = addressMap["city"] as? String
              val addressSubdivision = addressMap["subdivision"] as? String
              val addressPostalCode = addressMap["postalCode"] as? String
              val addressCountryCode = addressMap["countryCode"] as? String

              if (addressStreet1 != null){
                fieldsBuilder.field("addressStreet1", addressStreet1)
              }

              if (addressStreet2 != null){
                fieldsBuilder.field("addressStreet2", addressStreet2)
              }

              if (addressCity != null){
                fieldsBuilder.field("addressCity", addressCity)
              }

              if (addressSubdivision != null){
                fieldsBuilder.field("addressSubdivision", addressSubdivision)
              }

              if (addressPostalCode != null){
                fieldsBuilder.field("addressPostalCode", addressPostalCode)
              }

              if (addressCountryCode != null){
                fieldsBuilder.field("addressCountryCode", addressCountryCode)
              }
            }

            if (emailAddress != null){
              fieldsBuilder.field("emailAddress", emailAddress)
            }

            if (phoneNumber != null){
              fieldsBuilder.field("phoneNumber", phoneNumber)
            }

            if (birthdate != null){
              fieldsBuilder.field("birthdate", birthdate)
            }

            if(additionalFields != null) {
              for ((key, value) in additionalFields) {
                when(value) {
                  is Int -> fieldsBuilder.field(key, value)
                  is String -> fieldsBuilder.field(key, value)
                  is Boolean -> fieldsBuilder.field(key, value)
                }
              }
            }
          }

          val inquiry = Inquiry.fromTemplate(templateId)
                  .accountId(accountId)
                  .referenceId(referenceId)
                  .fields(fieldsBuilder.build())

          if (environment != null) {
            inquiry.environment(Environment.valueOf(environment.toUpperCase()));
          }

          println("**************************")
          println(inquiry)
          println(fieldsBuilder.build())

          inquiry.build().start(activity, requestCode)
          result.success("Inquiry started with templateId")
        }
      }
      else -> result.notImplemented()
    }
  }

  /// - ActivityResultListener interface

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (requestCode == requestCode) {
      when(val result = Inquiry.onActivityResult(data)) {
        is InquiryResponse.Complete -> {
          println("***********************************************************")
          println(result.status)
          if (result.status == "completed"){
            val arguments = hashMapOf<String, Any?>();
            arguments["inquiryId"] = result.inquiryId;
            println(result.fields);
            channel.invokeMethod("onSuccess", arguments);
            return true;
          }

          if (result.status == "failed"){
            val arguments = hashMapOf<String, Any?>();
            arguments["inquiryId"] = result.inquiryId;
            channel.invokeMethod("onFailed", arguments);
            return true;
          }
          if (result.status == "pending"){
            val arguments = hashMapOf<String, Any?>();
            arguments["inquiryId"] = result.inquiryId;
            channel.invokeMethod("onPending", arguments);
            return true;
          }
        }

        is InquiryResponse.Cancel -> {
          channel.invokeMethod("onCancelled", null);
          return true;
        }
        is InquiryResponse.Error -> {
          val arguments = hashMapOf<String, Any?>();
          arguments["error"] = result.debugMessage;
          channel.invokeMethod("onError", arguments);
          return true;
        }
      }
    }
    return false;
  }

  /// - Helpers

  private fun attributesToMap(attributes: Map<String, InquiryField>): HashMap<String, Any?> {
    val result = hashMapOf<String, Any?>();
    val nameMap = hashMapOf<String, Any?>();
    val addressMap = hashMapOf<String, Any?>();

    nameMap["first"] = attributes["nameFirst"];
    nameMap["middle"] = attributes["nameMiddle"];
    nameMap["last"] = attributes["nameLast"];

    addressMap["street1"] = attributes["addressStreet1"];
    addressMap["street2"] = attributes["addressStreet2"];
    addressMap["city"] = attributes["addressCity"];
    addressMap["subdivision"] = attributes["addressSubdivision"];
    addressMap["subdivisionAbbr"] = attributes["addressSubdivisionAbbr"];
    addressMap["postalCode"] = attributes["addressPostalCode"];
    addressMap["countryCode"] = attributes["addressCountryCode"];

    result["name"] = nameMap;
    result["address"] = addressMap;

    if (attributes["birthdate"] is Date) {
      val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
      result["birthdate"] = formatter.format(attributes["birthdate"]);
    }

    return result;
  }


  /// - ActivityAware interface

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    this.binding = binding;
    this.activity = binding.activity;
    binding.addActivityResultListener(this);
  }

  override fun onDetachedFromActivity() {
    this.binding?.removeActivityResultListener(this);
    this.activity = null;
    this.binding = null;
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding);
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }
}
