/************************************************************
Copyright (c) 2005, 2010, 2015, Curtis Bartley and Golimojo Project Contributors.
All rights reserved.
 
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 
* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
 
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
 
* Neither the name of the Golimojo Project, Curtis Bartley, nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.
 
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
************************************************************/

// ------------------------------------------------------------
// ----------------------- class Linker -----------------------
// ------------------------------------------------------------

// ---------------------------------------- Linker constructor

function Linker(targetDoc, pageTitleList)
{
    this.targetDoc = targetDoc;
    this.treatAsLeafNodeNameList = ["A", "SCRIPT", "STYLE"];
    this.textNodeIterator = new TextNodeIterator(targetDoc.body, this.treatAsLeafNodeNameList);
    this.nextTextNode = this.textNodeIterator.next();
    this.textNodeLinker = new TextNodeLinker(pageTitleList);
    this.doLinking = false;
    this.startDelayMs = 10;
    this.delayMs = 50;
}

// ---------------------------------------- Linker engage

Linker.prototype.engage = function ()
{
    Styler.styler.insertStyleSheet(this.targetDoc);
    this.startLinking();
}

// ---------------------------------------- Linker disengage

Linker.prototype.disengage = function ()
{
    this.stopLinking();
    this.textNodeLinker.removeOurLinks(this.targetDoc);
}

// ---------------------------------------- Linker startLinking

Linker.prototype.startLinking = function ()
{
    function timerFun()
    {
        return this.doSomeLinking(this.stopTimerFlag);
    }

    if (!this.doLinking)
    {
        this.doLinking = true;
        this.runOnTimer(this, timerFun, this.startDelayMs, this.delayMs);
    }
}

// ---------------------------------------- Linker stopLinking

Linker.prototype.stopLinking = function ()
{
    this.doLinking = false;
}

// ---------------------------------------- Linker runOnTimer

Linker.prototype.stopTimerFlag = "stop-timer-flag";

Linker.prototype.runOnTimer = function (receiver, timerFun, startDelayMs, delayMs)
{
    var stopTimerFlag = this.stopTimerFlag;
    var onTimerCallback = Tools.createCallback(onTimer, this);

    function onTimer()
    {
        if (!this.doLinking) return;
        var flag = timerFun.call(receiver);
        if (flag == stopTimerFlag) return;
        setTimeout(onTimerCallback, delayMs);
    }

    setTimeout(onTimerCallback, startDelayMs);
}

// ---------------------------------------- Linker doSomeLinking

Linker.prototype.doSomeLinking = function (doneFlag)
{
    var runTimeMs = 10;
    var stopTime = Date.now() + runTimeMs;

    while (Date.now() < stopTime)
    {
        var flag = this.doSomeLimitedLinking(doneFlag);
        if (flag == doneFlag) return doneFlag;
    }
    
    return null;
}

// ---------------------------------------- Linker doSomeLimitedLinking

Linker.prototype.doSomeLimitedLinking = function (doneFlag)
{
    // Text node linking is destructive, so we want to get the next
    // node *before* we do anything to the current one.
    var textNode = this.nextTextNode;
    if (textNode == null) return doneFlag;
    this.nextTextNode = this.textNodeIterator.next();
    
    // Link this text node.
    this.textNodeLinker.linkTextNode(textNode, this.linkList);
    if (this.nextTextNode == null) return doneFlag;
    return null;
}

// ------------------------------------------------------------
// ----------------------- class Styler -----------------------
// ------------------------------------------------------------

// ---------------------------------------- Styler class variables

Styler.styler = new Styler();

// ---------------------------------------- Styler constructor

function Styler()
{
    this.linkStyle = null;
    this.linkColor = null;
}

// ---------------------------------------- Styler updateStyles

Styler.prototype.updateStyles = function (windowList, linkStyle, linkColor)
{
    this.linkStyle = linkStyle;
    this.linkColor = linkColor;
    for (var i = 0; i < windowList.length; i++)
    {
        var targetDoc = windowList[i].document;
        Styler.styler.insertStyleSheet(targetDoc);
    }       
}

// ---------------------------------------- Styler insertStyleSheet

Styler.prototype.insertStyleSheet = function (targetDoc)
{
    if (this.linkStyle == null || this.linkColor == null) return;

    // Remove the old stylesheet, if any.   
    var styleSheetId = "golimojo-stylesheet";
    var styleSheetElement = targetDoc.getElementById(styleSheetId);
    if (styleSheetElement != null)
    {
        styleSheetElement.parentNode.removeChild(styleSheetElement);
    }

    // Create a new style sheet element.
    var styleText = this.createStyleSheetText(this.linkStyle, this.linkColor);
    styleSheetElement = targetDoc.createElement("STYLE");
    styleSheetElement.id = styleSheetId;
    styleSheetElement.textContent = styleText;

    // Figure out where to insert the stylesheet.
    var styleParent = targetDoc.body;
    var elemList = targetDoc.getElementsByTagName("HEAD");
    if (elemList.length > 0)
    {
        styleParent = elemList[0];
    }
    
    // Insert it.
    styleParent.appendChild(styleSheetElement);
}

// ---------------------------------------- Styler insertStyleSheet

Styler.prototype.createStyleSheetText = function (linkStyle, linkColor)
{
    // Build the style template as a list of lines.
    var styleTextLines = [];
    styleTextLines.push("a.golimojo-wikipedia-link:link, ");
    styleTextLines.push("a.golimojo-wikipedia-link:visited,");
    styleTextLines.push("a.golimojo-wikipedia-link:hover,");
    styleTextLines.push("a.golimojo-wikipedia-link:active");
    styleTextLines.push("{");
    if (linkStyle == "strong")
    {
        styleTextLines.push("   font-weight: bold;");
    }
    if (linkStyle != "low-profile")
    {
        styleTextLines.push("   color: #color#;");
        styleTextLines.push("   text-decoration: underline;");
    }
    if (linkStyle == "low-profile")
    {
        styleTextLines.push("   color: inherit;");
        styleTextLines.push("   text-decoration: none;");
        styleTextLines.push("   border-bottom: dashed #color# 1px;");
    }
    styleTextLines.push("}");
    
    // Concatentate the lines and substitute in the link color.
    var styleText = styleTextLines.join("\n");
    styleText = styleText.replace(/#color#/g, linkColor);

    // Return the style text.   
    return styleText;
}

// ------------------------------------------------------------
// ------------------ class TextNodeIterator ------------------
// ------------------------------------------------------------

// ---------------------------------------- TextNodeIterator constructor

function TextNodeIterator(rootElem, treatAsLeafNodeNameList)
{
    this.rootNode = rootElem;
    this.lastNode = rootElem;
    this.treatAsLeafNodeNameBag = this.createBagFromList(treatAsLeafNodeNameList, "");
}

// ---------------------------------------- TextNodeIterator next

TextNodeIterator.prototype.next = function ()
{
    this.lastNode = this.nextTextNode(this.lastNode);
    return this.lastNode;
}

// ---------------------------------------- TextNodeIterator createBagFromList

TextNodeIterator.prototype.createBagFromList = function (list, defaultValue)
{
    var bag = {};
    for (var i = 0; i < list.length; i++)
    {
        var key = list[i];
        bag[key] = defaultValue;
    }
    return bag;
}

// ---------------------------------------- TextNodeIterator nextTextNode

TextNodeIterator.prototype.nextTextNode = function (node)
{
    while (true)
    {
        node = this.nextNode(node);
        if (node == null) return null;
        if (node.nodeType == 3) return node;
    }
}

// ---------------------------------------- TextNodeIterator nextNode

TextNodeIterator.prototype.nextNode = function (node)
{
    // If a node has children then its first child is the "next" node
    // for an in-order traversal, which is what we're doing here.
    // However, we treat some kinds of nodes as leaves, even if the
    // have children, so we need to special case for those.
    if (this.treatAsLeafNodeNameBag[node.nodeName] == null)
    {
        if (node.firstChild != null) return node.firstChild;
    }

    // Otherwise, the next node is the current node's next sibling.
    // If it doesn't have a next sibling then walk up the parent
    // chain towards the root node, looking for the first one that
    // *does* have a sibling.  That sibling is our next node.
    while (node != null)
    {
        if (node == this.rootNode) break;
        if (node.nextSibling != null) return node.nextSibling;
        node = node.parentNode;
    }

    // We didn't find a sibling, so we're done -- return null.
    return null;
}

// ------------------------------------------------------------
// ------------------- class TextNodeLinker -------------------
// ------------------------------------------------------------

// ---------------------------------------- TextNodeLinker constructor

function TextNodeLinker(pageTitleList)
{
    this.pageTitleStore = new PageTitleStore(pageTitleList);
}

// ---------------------------------------- TextNodeLinker linkTextNode

TextNodeLinker.prototype.linkTextNode = function (textNode)
{
    // Generally you'd expect text nodes to always have a parent.  Empirically,
    // however, this is not always the case.  If there's no owner, just skip
    // this text node and keep going.
    if (textNode.ownerDocument == null) return;

    var tokenList = this.pageTitleStore.parseTextIntoTokens(textNode.textContent);
    var cutList = this.findPageTitleCuts(tokenList);
    var nodeList = this.createNodesFromCutList(textNode.ownerDocument, tokenList, cutList);
    this.replaceNodeWithNodes(textNode, nodeList);
}

// ---------------------------------------- TextNodeLinker removeOurLinks

TextNodeLinker.prototype.removeOurLinks = function (targetDoc)
{
    // Build a list of all of the anchor elements that we've added.
    var ourAnchorList = [];
    var anchorList = targetDoc.getElementsByTagName("A");
    for (var i = 0; i < anchorList.length; i++)
    {
        var anchor = anchorList[i];
        if (anchor.className == "golimojo-wikipedia-link")
        {
            ourAnchorList.push(anchor);
        }
    }

    // Replace each of our anchors with its respective contents.
    // Note that getElementsByTagName returns a "live" list that
    // changes dynamically to reflect changes in the document, so
    // we do *not* want to use it here.
    for (var i = 0; i < ourAnchorList.length; i++)
    {
        var anchor = ourAnchorList[i];
        this.replaceNodeWithNodes(anchor, anchor.childNodes);
    }
}

// ---------------------------------------- TextNodeLinker findPageTitleCuts
// Find the page titles in the token list, then return a cut list indicating
// which parts are page titles and which aren't.  The cuts in the cut list
// are in order, they completely cover the token list, and they don't overlap.
// However, some cuts may be degenerate "empty" cuts.  If a cut matches a
// page title, that page title is stored in the cut object's "data" member.
TextNodeLinker.prototype.findPageTitleCuts = function (tokenList)
{
    var index = 0;
    var cutList = [];
    while (index < tokenList.length)
    {
        var i = 0; 
        var matchPair = null;
        for (i = index; i < tokenList.length; i++)
        {
            matchPair = this.pageTitleStore.matchTokensAt(tokenList, i);
            if (matchPair != null) break;
        }
        
        cutList.push(new TokenListCut(index, i, null));
        
        var matchCount = 0;
        if (matchPair != null)
        {
            var pageTitle = matchPair[0];
            matchCount = matchPair[1];
            cutList.push(new TokenListCut(i, i + matchCount, pageTitle));
        }
        
        index = i + matchCount;
    }
    
    return cutList;
}

// ---------------------------------------- TextNodeLinker createNodesFromCutList

TextNodeLinker.prototype.createNodesFromCutList = function (ownerDocument, tokenList, cutList)
{
    var nodeList = [];
    for (var i = 0; i < cutList.length; i++)
    {
        var cut = cutList[i];
        var node = this.createNodeFromCut(ownerDocument, tokenList, cut);
        if (node != null) 
        {
            nodeList.push(node);
        }
    }
    return nodeList;
}

// ---------------------------------------- TextNodeLinker createNodeFromCut

TextNodeLinker.prototype.createNodeFromCut = function (ownerDocument, tokenList, cut)
{
    // Create a text node for the cut, and if there is no
    // associated page title, just return the text node.
    if (cut.isEmpty()) return null;
    var textNode = cut.createTextNode(ownerDocument, tokenList);
    if (cut.data == null) return textNode;

    // If there is a page title, wrap an anchor around the
    // text node and return the anchor.
    var pageTitle = cut.data;
    var anchor = ownerDocument.createElement("A");
    anchor.className = "golimojo-wikipedia-link";
    anchor.appendChild(textNode);
    anchor.href = this.createWikipediaLink(pageTitle);
    return anchor;      
}

// ---------------------------------------- TextNodeLinker replaceNodeWithNodes

TextNodeLinker.prototype.replaceNodeWithNodes = function (node, replacementNodeList)
{
    for (var i = 0; i < replacementNodeList.length; i++)
    {
        var replacementNode = replacementNodeList[i];
        node.parentNode.insertBefore(replacementNode, node);
    }
    node.parentNode.removeChild(node);
}

// ---------------------------------------- TextNodeLinker createWikipediaLink

TextNodeLinker.prototype.createWikipediaLink = function (pageTitle)
{
    var escapedPageTitle = escape(pageTitle.replace(" ", "_"));
    var url = "http://en.wikipedia.org/wiki/" + escapedPageTitle;
    return url;
}

// ------------------------------------------------------------
// -------------------- class TokenListCut --------------------
// ------------------------------------------------------------

// ---------------------------------------- TokenListCut constructor

function TokenListCut(startIndex, endIndex, data)
{
    this.startIndex = startIndex; 
    this.endIndex = endIndex;
    this.data = data;
}

// ---------------------------------------- TokenListCut isEmpty

TokenListCut.prototype.isEmpty = function ()
{
    if (this.startIndex == this.endIndex) return true;
    return false;
}

// ---------------------------------------- TokenListCut createTextNode

TokenListCut.prototype.createTextNode = function (ownerDocument, tokenList)
{   
    var cutTokenList = tokenList.slice(this.startIndex, this.endIndex);
    var cutText = cutTokenList.join("");
    var textNode = ownerDocument.createTextNode(cutText);
    return textNode;
}

// ------------------------------------------------------------
// ------------------- class PageTitleStore -------------------
// ------------------------------------------------------------

// ---------------------------------------- PageTitleStore constructor

function PageTitleStore(pageTitleList)
{
    this.matcher = new Matcher([WhitespaceFragment, NumbersFragment, LettersFragment], SymbolsFragment);
    this.pageTitlePrefixBag = {};
    this.pageTitleBag = {};
    this.storePageTitleList(pageTitleList);
}

// ---------------------------------------- PageTitleStore storePageTitleList

PageTitleStore.prototype.storePageTitleList = function (pageTitleList)
{
    for (var i = 0; i < pageTitleList.length; i++)
    {
        var pageTitle = pageTitleList[i];
        this.storePageTitle(pageTitle);
    }
}

// ---------------------------------------- PageTitleStore storePageTitle

PageTitleStore.prototype.storePageTitle = function (pageTitle)
{
    // Store the prefixes.
    var prefix = "";
    var fragmentList = this.matcher.tokenize(pageTitle);
    for (var i = 0; i < fragmentList.length; i++)
    {
        var fragment = fragmentList[i];
        prefix = prefix + fragment.toCanonicalString();
        this.pageTitlePrefixBag[prefix] = "";
    }
    
    // Store the page title itself.
    var pageTitleNormalized = prefix;
    this.pageTitleBag[pageTitleNormalized] = pageTitle;
}

// ---------------------------------------- PageTitleStore matchTokensAt

PageTitleStore.prototype.matchTokensAt = function (tokenList, index)
{
    var phraseText = "";
    var longestMatchPair = null;
    for (var i = index; i < tokenList.length; i++)
    {
        var token = tokenList[i];
        phraseText = phraseText + token.toCanonicalString();
        if (this.pageTitlePrefixBag[phraseText] == null) break;
        var pageTitle = this.pageTitleBag[phraseText];
        if (pageTitle != null && typeof(pageTitle) == "string")
        {
            var matchCount = i + 1 - index;
            longestMatchPair = [pageTitle, matchCount];
        }
    }
    return longestMatchPair;
}

// ---------------------------------------- PageTitleStore parseTextIntoTokens

PageTitleStore.prototype.parseTextIntoTokens = function (text)
{
    var tokenList = this.matcher.tokenize(text);
    return tokenList;
}

// ------------------------------------------------------------
// ---------------- text fragment/token classes ---------------
// ------------------------------------------------------------

// ---------------------------------------- Subclass
// "Subclass(Derived, Base)" is roughly analogous to the idiomatic
// "Derived.prototype = new Base(...)", except that the constructor
// property is properly fixed up and no dummy Base instance is
// needed for the prototype.
function Subclass(derivedConstructor, baseConstructor)
{
    protoConstructor.prototype = baseConstructor.prototype;
    protoConstructor.prototype.constructor = baseConstructor;
    function protoConstructor()
    {
    }
    
    derivedConstructor.prototype = new protoConstructor();
    derivedConstructor.prototype.constructor = derivedConstructor;
}

// ---------------------------------------- class TextFragment

TextFragment.rpToken = null;

function TextFragment(text)
{
    this.text = text;
}

TextFragment.prototype.toString = function ()
{
    return this.text;
}

TextFragment.prototype.toCanonicalString = function ()
{
    return this.toString();
}

// ---------------------------------------- class WhitespaceFragment

Subclass(WhitespaceFragment, TextFragment);

WhitespaceFragment.rpToken = "[ \\t\\r\\n]+";

function WhitespaceFragment(text)
{
    TextFragment.call(this, text);
}

WhitespaceFragment.prototype.toCanonicalString = function ()
{
    return " ";
}

// ---------------------------------------- class NumbersFragment

Subclass(NumbersFragment, TextFragment);

NumbersFragment.rpToken = "[0-9]+";

function NumbersFragment(text)
{
    TextFragment.call(this, text);
}

// ---------------------------------------- class LettersFragment

Subclass(LettersFragment, TextFragment);

LettersFragment.rpToken = "[A-Za-z]+";

function LettersFragment(text)
{
    TextFragment.call(this, text);
}

// ---------------------------------------- class SymbolsFragment

Subclass(SymbolsFragment, TextFragment);

SymbolsFragment.rpToken = null;

function SymbolsFragment(text)
{
    TextFragment.call(this, text);
}

// ------------------------------------------------------------
// ----------------------- class Matcher ----------------------
// ------------------------------------------------------------

// ---------------------------------------- Matcher constructor

function Matcher(tokenTypeList, defaultTokenType)
{
    // Save a copy of the token type list.
    this.tokenTypeList = tokenTypeList.slice();

    // Derive a list of token patterns from our list of token types.
    var rpTokenList = this.createTokenPatternList(tokenTypeList);

    // Define our temporary placekeeper characters.
    this.keyCh = "\x01";
    this.splitCh = "\x02";

    // Create the master key which will be used to generate unique token-type keys.
    this.keySize = rpTokenList.length + 1;
    this.masterKey = this.createMasterKey(this.keyCh, this.keySize);

    // Build a composite pattern that will recognize text matching any of the tokens.
    var rpTokens = this.createCompositeOrPattern(rpTokenList);
    this.rxTokens = new RegExp(rpTokens, "g");

    // Build a composite pattern that will recognize text matching any of the *keyed* patterns. 
    var rpKeyedTokens = this.createKeyedCompositeOrPattern(rpTokenList, this.masterKey);

    // Build a pattern which will match a keyed pattern then any extra trailing key characters.
    var rpKeyedTokenThenKeyExcess = rpKeyedTokens + this.keyCh + "*";
    this.rxKeyedTokenThenKeyExcess = new RegExp(rpKeyedTokenThenKeyExcess, "g");

    // Build a pattern that identifies the character right before a transition from a key
    // to a token/non-token run or the transition from a token/non-token run to a key.
    var rpTransition1 = "(k(?!k))".replace(/k/g, this.keyCh);
    var rpTransition2 = "([^k](?=k))".replace(/k/g, this.keyCh);
    var rpTransition = "(" + rpTransition1 + "|" + rpTransition2 + ")";
    this.rxTransition = new RegExp(rpTransition, "g");

    // Non-token runs can be represented by Text nodes so put a mapping in from 
    // master keys (representing non-token runs) to Text nodes.
    Test.assert(this.tokenTypeList[this.masterKey.length - 1] == null, "The master key must be big enough!");
    this.tokenTypeList[this.masterKey.length - 1] = defaultTokenType;
}

// ---------------------------------------- Matcher tokenize

Matcher.prototype.tokenize = function (text)
{
    // Insert master keys before and after every token and also before and after the text as a whole.
    // ... Two adjacent master keys will occur on occasion but they will be treated implicitly as if
    // ... they were a single master key.  An extraneous master key (or master key pair) will always
    // ... be present at the very beginning of the list.  After that the text will consist of 
    // ... alternating text runs and keys.  Each run will be either a single token or a run of text
    // ... which contains no tokens at all.
    text = this.masterKey + text.replace(this.rxTokens, this.masterKey + "$1" + this.masterKey) + this.masterKey;

    // Replace each master key with a shorter token key which identifies each token-type.
    // ... Non-token runs will always be followed by a single master key, and this step
    // ... will not change these master keys -- so master keys will identify non-token
    // ... runs and unique (shorter) token keys will identify each token.
    text = text.replace(this.rxKeyedTokenThenKeyExcess, "$1" /* token and unique key */ );

    // Insert split characters at boundaries.
    text = text.replace(this.rxTransition, "$1" /* char before transition */ + this.splitCh);

    // Split the text into an array.  Even indices (remember we start at zero) will
    // hold tokens/non-token runs, and odd indices will hold keys.  Note that we may
    // have an extra key at the end, which we can just ignore.
    var tokensAndKeysList = text.split(this.splitCh);

    // Create tokens for each key/text pair and put them in a list.
    var tokenList = [];
    for (var i = 1; i + 1 < tokensAndKeysList.length; i += 2)
    {
        var tokenText = tokensAndKeysList[i];
        var key = tokensAndKeysList[i + 1];
        var nodeCtor = this.tokenTypeList[key.length - 1];
        var tokenNode = new nodeCtor(tokenText);
        tokenList.push(tokenNode);
    }

    // Return the list of tokens.
    return tokenList;
}

// ---------------------------------------- Matcher createKeyedCompositeOrPattern
// Create a composite regular expression pattern which will match any text
// that any of the "keyed" patterns derived from "rpTokenList" will match.
Matcher.prototype.createKeyedCompositeOrPattern = function (rpTokenList, masterKey)
{
    var rpKeyedTokenList = this.createKeyedTokenPatterns(rpTokenList, masterKey);
    return this.createCompositeOrPattern(rpKeyedTokenList);
}

// ---------------------------------------- Matcher createKeyedTokenPatterns
// Create and return a new list of regular expression patterns based on
// the patterns in "rpTokenList".  Each output pattern will match any string
// which matches the input pattern and which is followed by a unique key.
// Keys are sequences of a single key character; they are unique simply
// by differing in length.  The key for position "i" in the output 
// list will always be "i + 1" characters long.
Matcher.prototype.createKeyedTokenPatterns = function (rpTokenList, masterKey)
{
    var rpKeyedTokenList = [];
    for (var i = 0; i < rpTokenList.length; i++)
    {
        var key = this.createKey(masterKey, i + 1);
        var rpKeyedToken = "((" + rpTokenList[i] + ")" + key + ")";
        rpKeyedTokenList.push(rpKeyedToken);
    }
    return rpKeyedTokenList;
}

// ---------------------------------------- Matcher createKey

Matcher.prototype.createKey = function (masterKey, size)
{
    Test.assert(size <= masterKey.length, "The master key must be large enough!");
    var key = masterKey.substring(0, size);
    return key;
}

// ---------------------------------------- Matcher createMasterKey

Matcher.prototype.createMasterKey = function (keyCh, size)
{
    var key = "";
    for (var i = 0; i < size; i++)
    {
        key += keyCh;
    }
    return key;
}

// ---------------------------------------- Matcher createCompositeOrPattern

Matcher.prototype.createCompositeOrPattern = function (rpTokenList)
{
    return "((" + rpTokenList.join(")|(") + "))";
}

// ---------------------------------------- Matcher createTokenPatternList

Matcher.prototype.createTokenPatternList = function (tokenTypeList)
{
    var rpTokenList = [];
    for (var i = 0; i < tokenTypeList.length; i++)
    {
        var rpToken = tokenTypeList[i].rpToken;
        rpTokenList.push(rpToken);
    }
    return rpTokenList;
}

// ---------------------------------------- Matcher createTokenType 

Matcher.prototype.createTokenType = function (ctorFunction, rpToken)
{
    ctorFunction.rpToken = rpToken;
    return ctorFunction;
}
