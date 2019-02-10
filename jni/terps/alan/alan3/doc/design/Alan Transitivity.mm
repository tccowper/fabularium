<map version="0.9.0">
<!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
<node CREATED="1452269655844" ID="ID_99702451" MODIFIED="1452271160332" TEXT="Alan Transitivity">
<node CREATED="1452269688945" HGAP="16" ID="ID_1548665888" MODIFIED="1452271198228" POSITION="right" TEXT="At location" VSHIFT="-32">
<arrowlink DESTINATION="ID_514223295" ENDARROW="Default" ENDINCLINATION="9;-3;" ID="Arrow_ID_1624397932" STARTARROW="None" STARTINCLINATION="51;21;"/>
<node CREATED="1452269695289" HGAP="47" ID="ID_1130853784" MODIFIED="1452271220646" TEXT="[Transitively] At location" VSHIFT="-1">
<node CREATED="1452271840807" ID="ID_904834424" MODIFIED="1452272396342">
<richcontent TYPE="NODE"><html>
  <head>
    
  </head>
  <body>
    <p>
      directly or indirectly
    </p>
  </body>
</html>
</richcontent>
</node>
</node>
<node CREATED="1452269710587" HGAP="46" ID="ID_1249978977" MODIFIED="1452271217702" TEXT="Directly At location" VSHIFT="-4">
<node CREATED="1452271671353" ID="ID_1126445065" MODIFIED="1452360185811">
<richcontent TYPE="NODE"><html>
  <head>
    
  </head>
  <body>
    <p>
      l = positionOf(instance)
    </p>
    <p>
      if l isa location: true
    </p>
    <p>
      else false
    </p>
  </body>
</html>
</richcontent>
</node>
</node>
<node CREATED="1452269718843" HGAP="51" ID="ID_844927138" MODIFIED="1452271213938" TEXT="Indirectly At location">
<node CREATED="1452271705295" ID="ID_704232967" MODIFIED="1452360225291">
<richcontent TYPE="NODE"><html>
  <head>
    
  </head>
  <body>
    <p>
      l = positionOf(instance)
    </p>
    <p>
      if l != location or l ! isa location: false
    </p>
    <p>
      else while l != null:
    </p>
    <p>
      &#160;&#160;l = positionOf(l)
    </p>
    <p>
      &#160;&#160;if l == location: true
    </p>
    <p>
      false
    </p>
  </body>
</html>
</richcontent>
</node>
</node>
</node>
<node CREATED="1452269766313" ID="ID_514223295" MODIFIED="1452271121085" POSITION="right" TEXT="Here">
<arrowlink DESTINATION="ID_1548665888" ENDARROW="Default" ENDINCLINATION="47;24;" ID="Arrow_ID_959211603" STARTARROW="None" STARTINCLINATION="9;-9;"/>
<node CREATED="1452272478591" ID="ID_1131511108" MODIFIED="1452272491141" TEXT="== At current location"/>
</node>
<node CREATED="1452270815529" HGAP="12" ID="ID_877477941" MODIFIED="1452271192095" POSITION="right" TEXT="At non-location" VSHIFT="36">
<node CREATED="1452270824909" ID="ID_1357269982" MODIFIED="1452270839806" TEXT="[Transitively] At non-location"/>
<node CREATED="1452270840520" ID="ID_922301943" MODIFIED="1452270849094" TEXT="Directly At non-location">
<node CREATED="1452272517798" ID="ID_1527172710" MODIFIED="1452360241680" TEXT="positionOf(instance) == positionOf(non-location)"/>
</node>
<node CREATED="1452270850191" ID="ID_86809784" MODIFIED="1452270863611" TEXT="Indirectly At non-location">
<node CREATED="1452272556286" ID="ID_1364508448" MODIFIED="1452272556286" TEXT=""/>
</node>
</node>
<node CREATED="1452270867807" HGAP="-21" ID="ID_1918936854" MODIFIED="1452271193721" POSITION="right" TEXT="In instance" VSHIFT="27">
<node CREATED="1452270873754" ID="ID_1303879216" MODIFIED="1452270884916" TEXT="[Transitively] In instance"/>
<node CREATED="1452270885519" ID="ID_449692980" MODIFIED="1452270892180" TEXT="Directly In instance">
<node CREATED="1452271919410" ID="ID_1808550107" MODIFIED="1452360259237" TEXT="The position of an instance, which must not be a location, must be the non-location instance indicated"/>
</node>
<node CREATED="1452270892807" ID="ID_863274978" MODIFIED="1452270902277" TEXT="Indirectly In instance">
<node CREATED="1452272781673" ID="ID_1001728844" MODIFIED="1452359879266" TEXT=""/>
</node>
</node>
<node CREATED="1452271161126" HGAP="69" ID="ID_233790346" MODIFIED="1452271182788" POSITION="left" TEXT="Near" VSHIFT="89"/>
</node>
</map>
