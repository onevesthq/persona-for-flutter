import 'package:flutter/material.dart';
import 'package:persona_flutter/persona_flutter.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Inquiry _inquiry;

  @override
  void initState() {
    super.initState();

    _inquiry = Inquiry(
      configuration: TemplateIdConfiguration(
        templateId: "ENTER-YOUR-TEMPLATE-ID-HERE",
        environment: InquiryEnvironment.sandbox,
        referenceId: "test-ref",
        iOSTheme: InquiryTheme(
          accentColor: Color(0xff22CB8E),
          primaryColor: Color(0xff22CB8E),
          buttonBackgroundColor: Color(0xff22CB8E),
          darkPrimaryColor: Color(0xff167755),
          buttonCornerRadius: 8,
          textFieldCornerRadius: 0,
        ),
      ),
      onSuccess: (
        String inquiryId,
      ) {
        print("onSuccess");
        print("- inquiryId: $inquiryId");
      },
      onFailed: (
        String inquiryId,
      ) {
        print("onFailed");
        print("- inquiryId: $inquiryId");
      },
      onCancelled: () {
        print("onCancelled");
      },
      onError: (String error) {
        print("onError");
        print("- $error");
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: Container(
          color: Colors.grey[200],
          child: Center(
            child: ElevatedButton(
              onPressed: () {
                _inquiry.start();
              },
              child: Text("Start Inquiry"),
            ),
          ),
        ),
      ),
    );
  }
}
