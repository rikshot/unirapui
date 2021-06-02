use warp::{Filter, ws::Ws, ws::Message};

use log::error;

use futures::StreamExt;
use futures::SinkExt;

const SERVER_KEY: &str = include_str!("../certs/server.key");
const SERVER_CERT: &str = include_str!("../certs/server.crt");
const CA_CERT: &str = include_str!("../certs/ca.crt");

pub fn greeting(name: &str) -> String {
    format!("Hello, {}!", name)
}

pub async fn start(index: String, port: u16) {
    let root = warp::get()
        .and(warp::filters::path::end())
        .map(move || index.clone())
        .with(warp::filters::reply::header("Content-Type", "text/html"));

    let ws = warp::path("ws").and(warp::ws()).map(|ws: Ws| {
        ws.on_upgrade(|websocket| async {
            let (mut tx, mut rx) = websocket.split();
            while let Some(result) = rx.next().await {
                let message = match result {
                    Ok(message) => message,
                    Err(error) => {
                        error!("websocket error: {}", error);
                        break;
                    }
                };
                if let Ok(message) = message.to_str() {
                    tx.send(Message::text(greeting(message))).await.unwrap();
                }
            }
        })
    });

    warp::serve(root.or(ws))
        .tls()
        .key(SERVER_KEY)
        .cert(SERVER_CERT)
        .client_auth_required(CA_CERT)
        .run(([127, 0, 0, 1], port)).await;
}
