package com.aaronbrecher.neverlate.models

class HereApiBody(var locationDetails: List<EventLocationDetails> = ArrayList(),
                  var purchases: List<PurchaseData> = ArrayList())