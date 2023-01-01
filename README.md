# Green Feed Reader
Green Feed Readerは、BlackBerryスマートフォンからFeedlyを利用するためのアプリケーションです。BlackBerry 10も日本語入力がサポートされ魅力的になりましたが、やっぱりまだまだ9900を使いたい、そんな思いからこのアプリを作成しました。FeedlyからAPIの使用許可をいただいています。

ビューワー的な用途を想定して作成しました。ざっと記事の一覧をながめて、気になる記事をセーブ、後でPCなどの環境でじっくり読む、興味のないものはサクッと既読にする。そんなイメージで作成しました。

## 特徴
- BlackBerry OS 7スマートフォンから、Feedlyアカウントに蓄えられた記事を読むことができます。
- 記事の未読、既読状況を同期します。
- Savedタグの追加削除ができます。

## ネガティブな特徴
- フィード、タグ（Savedタグ除く）、カテゴリの追加、変更はできません。PC、他端末で環境を整えられた後にご利用ください。
- オフラインでの閲覧には対応していません。購読中のフィード名の表示を含め、インターネットへの接続が必要です。
- Feedly Proの機能、Feedlyを通したTwitter等の他サービスとの連携、検索はサポートしていません。

## 動作環境
- BlackBerry OS 7がインストールされたBlackBerryスマートフォン

## ご注意
本ソフトウエアを使用して発生した、物理的な破損、データ損失、金銭的な損失等、いかなる損害について一切責任を負いません。また、本ソフトウエアは無償で提供、ソースコードも公開しますので、ユーザーサポートはいたしません。ご理解をいただいた上でご利用ください。

## ダウンロード
OTA（Over-The-Air）のみで提供しています。下記リンクをBlackBerryの標準ブラウザからクリックすると、確認画面の後、インストールされます。確認画面でアプリケーション名を確認の上、ダウンロードをクリックしてください。インストールの前には、必ずお使いのBlackBerryのバックアップを取ってください。

[Green Feed Reader 0.3](https://ll0s0ll.github.io/blackberry/green-feed-reader/bin/GreenFeedReader.jad)

## バージョン情報
ver 0.3 (2015/07/02)
- ストリーム画面で、エントリー一覧の最後に「表示されているエントリーをすべて既読にするボタン（Mark all as read）」を出すようにしました。
- ストリーム画面で、’a’キーに「表示されているエントリーをすべて既読にする」ショートカットを設定しました。
- ストリーム画面で、該当するエントリーがないときに出していたアラート（No entries found）を出さないようにしました。
- マイナーバグフィックス

ver 0.2 (2014/08/31)
- ホーム画面の各カテゴリのリストを個別に折りたたみできるようにしました。折りたたまれている状態をデフォルトとしました。キーボードの’f’キー、メニューの’Toggle Show/Hide Feeds’から開閉できます。また、各カテゴリ間は、キーボードの’b’キーで最下段のカテゴリ、’n’キーで一つ下、’p’キーで一つ上、’t’キーで最上段のカテゴリへ移動するようにしました。
- ホーム画面のメニューに、’ALL’、’Saved’を追加しました。’ALL’は購読中のすべてのフィードの未読エントリーをストリーム画面で表示します。’Saved’はSavedタグが付けられたエントリーをストリーム画面で表示します。また、キーボードの’a’キーで’ALL’、’s’キーで’Saved’メニューと同様の動作をします。
- ストリーム画面、エントリー画面に’Send’メニューを追加しました。OS標準のsendmenu APIを使ったもので、”記事タイトル / フィード名 記事URL”の形式のテキストを、メール、BBM、Twitter等に送ることができます。※表示される送り先は、お使いのデバイスにインストールされているアプリにより異なります。
- アプリを終了する際に、確認のダイアログを出すようにしました。

ver 0.1 (2014/08/04)
- 公開

## 使い方
Green Feed Readerは4つの画面で構成されています。それぞれの画面とメニュー項目の説明です。メニュー項目についてはキーボードショートカットが割り当てられている場合は( )の中にキーを記載しています。

### 認証画面
![greenfeedreader_login](https://ll0s0ll.github.io/blackberry/green-feed-reader/img/greenfeedreader_login.jpg)

初回起動時、ログアウト後に表示されます。Feedlyでお使いのサービスよりログインしてください。ログインが成功するとホーム画面が表示されます。（同一サービス内でのアカウントの変更はできません。ex.一旦Googleアカウント1でログインしたものをGoogleアカウント2でログインしなおす）

### ホーム画面
![greenfeedreader_home](https://ll0s0ll.github.io/blackberry/green-feed-reader/img/greenfeedreader_home.png)

アプリ起動時に表示されます。（ログインしていない場合は認証画面が表示されます）購読中のフィードをカテゴリ別に表示します。カテゴリ内のフィードに未読エントリーがある場合は、そのエントリー数をカテゴリ名の前に表示します。それぞれのフィード名をクリックするとストリーム画面が表示されます。ALL項目は購読中のすべてのフィードの未読エントリーをストリーム画面で表示します。Saved For Later項目は、Savedタグが付けられたエントリーをストリーム画面で表示します。

#### メニュー項目
- ALL(a)  
  購読中のすべてのフィードの未読エントリーをストリーム画面で表示します。
- Saved(s)  
  Savedタグが付けられたエントリーをストリーム画面で表示します。
- Refresh(r)  
  未読件数、フィードの更新時間を更新します。
- Toggle Show/Hide Feeds(f)  
  カテゴリのフィードリストを開閉します。
- Reload  
  購読中のフィードを再取得します。
- Logout  
  ログアウトします。
  
### ストリーム画面
![greenfeedreader_stream](https://ll0s0ll.github.io/blackberry/green-feed-reader/img/greenfeedreader_stream.png)

フィードのエントリー一覧、まとめられたエントリー一覧を表示します。未読エントリーのみを表示している場合は、画面のタイトルの前に「(Unread)」と表示します。エントリーのタイトルが黒色のものは未読、灰色のものは既読を表します。Savedタグが付いたエントリーは更新時間の前に「Saved」が表示されます。それぞれのエントリーをクリックするとエントリー画面が開き、同時にクリックされたエントリーを既読にします。

#### メニュー項目
- Send  
  “記事タイトル / フィード名 記事URL”の形式のテキストを、メール、BBM、Twitter等に送ることができます。（表示される送り先は、お使いのデバイスにインストールされているアプリにより異なります）
- Get more entries  
  未表示のエントリーを読み込みます。（表示しきれないエントリーがある場合のみ表示されます）
- Refresh(r)  
  エントリーを再取得します。
- Toggle Unread/Read(m)  
  選択されたエントリーの未読/既読を切り替えます。
- Toggle Unsaved/Saved(s)  
  選択されたエントリーのSavedタグを切り替えます。
- Make all entries as Read(a)  
  表示されているすべてのエントリーを既読にします。
- Toggle Show/Hide Read  
  既読のエントリーを表示するかしないか切り替えます。
  
### エントリー画面
![greenfeedreader_entry](https://ll0s0ll.github.io/blackberry/green-feed-reader/img/greenfeedreader_entry.png)

エントリーの詳細を表示します。 Savedタグが付いたエントリーは更新時間の前に「Saved」が表示されます。tキーを押すと画面最上部、bキーを押すと画面最下部へ移動します。

#### メニュー項目
- Send  
  “記事タイトル / フィード名 記事URL”の形式のテキストを、メール、BBM、Twitter等に送ることができます。（表示される送り先は、お使いのデバイスにインストールされているアプリにより異なります）
- Next(n)  
  次のエントリーを表示します。（次のエントリーがある場合のみ表示されます）
- Prev(p)  
  前のエントリーを表示します。（前のエントリーがある場合のみ表示されます）
- Toggle Unsaved/Saved(s)  
  表示されているエントリーのSavedタグを切り替えます。
- Visit Website(space)  
  表示されているエントリーのWebサイトを標準ブラウザで表示します。
  
## スクリーンショット
![greenfeedreader_screenshot_home](https://ll0s0ll.github.io/blackberry/green-feed-reader/img/greenfeedreader_screenshot_home.png)

![greenfeedreader_screenshot_stream](https://ll0s0ll.github.io/blackberry/green-feed-reader/img/greenfeedreader_screenshot_stream.png)

![greenfeedreader_screenshot_entry](https://ll0s0ll.github.io/blackberry/green-feed-reader/img/greenfeedreader_screenshot_entry.png)

