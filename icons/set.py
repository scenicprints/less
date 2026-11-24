"""The icon set. Each icon is its own shape: no tile, the mark is the silhouette.

What holds them together is the grid (56, drawn inside the middle 48), one corner radius for
built rectangles (6), one stroke weight (4, thin detail 3), and flat colour. Marks are drawn in
the order given; the first is the body.
"""
from make import icon, fill, stroke, rect, circle, arc, wedge, gear, WHITE, INK, BLUE, RED, YELLOW, GREEN

R = 6  # the one corner radius for built rectangles


# -- the dock

# Phone: the blue disc with the handset, as the Pixel draws it.
icon("com.google.android.dialer", "phone", None,
     fill(circle(28, 28, 24), "#3B78E7"),
     fill("M19 16h7l3 8-4 2.5a17 17 0 0 0 7.5 7.5L35 30l8 3v7a3 3 0 0 1-3 3A24 24 0 0 1 16 19a3 3 0 0 1 3-3z", WHITE))

# Messages: the bubble itself.
icon("com.google.android.apps.messaging", "messages", None,
     fill("M12 8h32a6 6 0 0 1 6 6v22a6 6 0 0 1-6 6H24l-12 9V14a6 6 0 0 1 6-6z", "#2E6BD6"),
     fill(circle(19, 25, 3), WHITE), fill(circle(28, 25, 3), WHITE), fill(circle(37, 25, 3), WHITE))

# Brave: the shield.
icon("com.brave.browser", "brave", None,
     fill("M28 5l7 3 4-3 10 10-3 7 3 8-6 16-15 9-15-9-6-16 3-8-3-7 10-10 4 3z", "#FF5000"),
     stroke("M20 25l8 8 8-8M28 33v9", WHITE, 3))

# Apple Music: the red disc, the pair of notes.
icon("com.apple.android.music", "apple_music", None,
     fill(circle(28, 28, 24), "#FA2D48"),
     fill(circle(21, 36, 4), WHITE), fill(circle(35, 33, 4), WHITE),
     stroke("M25 36V19l14-3v17", WHITE, 3))

# Workouts, the user's own: a dumbbell.
icon("com.scenicprints.workouts", "workouts", None,
     fill(rect(5, 23, 46, 10, 5), "#26A69A"),
     fill(rect(10, 15, 9, 26, 4), "#26A69A"), fill(rect(37, 15, 9, 26, 4), "#26A69A"),
     fill(rect(4, 21, 5, 14, 2.5), "#1F8A80"), fill(rect(47, 21, 5, 14, 2.5), "#1F8A80"))

# -- the middle page

# Gmail: the envelope.
icon("com.google.android.gm", "gmail", None,
     fill(rect(6, 11, 44, 34, R), WHITE),
     stroke("M12 18v20", BLUE), stroke("M44 18v20", "#34A853"),
     stroke("M12 18l16 12 16-12", "#D93025"))

# Claude: the starburst.
icon("com.anthropic.claude", "claude", None,
     stroke("M28 6v44M6 28h44M12.4 12.4l31.2 31.2M12.4 43.6l31.2-31.2M28 6l-8 18M28 50l8-18M6 28l18 8M50 28l-18-8", "#D97757", 6))

# Camera: the lens.
icon("com.google.android.GoogleCamera", "camera", None,
     fill(circle(28, 28, 24), WHITE),
     stroke(arc(28, 28, 15, -90, 0), BLUE, 7), stroke(arc(28, 28, 15, 0, 90), RED, 7),
     stroke(arc(28, 28, 15, 90, 180), YELLOW, 7), stroke(arc(28, 28, 15, 180, 270), GREEN, 7),
     fill(circle(28, 28, 7), INK))

# Photos: the pinwheel.
icon("com.google.android.apps.photos", "photos", None,
     fill("M8 28a20 20 0 0 1 40 0z", RED),
     fill("M28 8a20 20 0 0 1 0 40z", BLUE),
     fill("M48 28a20 20 0 0 1-40 0z", YELLOW),
     fill("M28 48a20 20 0 0 1 0-40z", GREEN))

# Paylocity: the orange disc, a P.
icon("com.paylocity.paylocitymobile", "paylocity", None,
     fill(circle(28, 28, 24), "#FF6B00"),
     stroke("M21 40V16h9a7 7 0 0 1 0 14h-9", WHITE))

# BodyComp, the user's own: its runner on a green disc.
icon("com.scenicprints.bodycomp", "bodycomp", None,
     fill(circle(28, 28, 24), "#4ADE80"),
     fill(circle(31, 15, 3.5), "#0E0F12"),
     stroke("M19 26l7-5 7 5 7 5M26 21l2 12-6 10M28 33l8 4", "#0E0F12", 3.5))

# -- the Home deck

# Homey: the ring.
icon("app.homey", "homey", None,
     stroke(circle(28, 28, 17), "#1F5BFF", 9))

# myQ: the garage.
icon("com.chamberlain.android.liftmaster.myq", "myq", None,
     fill("M6 24L28 7l22 17v24H6z", "#0069C8"),
     fill(rect(14, 26, 28, 22, 3), WHITE),
     stroke("M18 33h20M18 40h20", "#0069C8", 3))

# Nest: the house.
icon("com.nest.android", "nest", None,
     fill("M6 27L28 8l22 19v21H6z", "#1A73E8"),
     fill(rect(23, 32, 10, 16, 2), WHITE))

# Hue: the bulb.
icon("com.philips.lighting.hue2", "hue", None,
     fill(circle(28, 23, 17), "#FFB300"),
     fill("M17 36h22v6a4 4 0 0 1-4 4H21a4 4 0 0 1-4-4z", INK),
     fill(rect(21, 47, 14, 4, 2), INK))

# iRobot: the green disc, the i.
icon("com.irobot.home", "irobot", None,
     fill(circle(28, 28, 24), "#6CC04A"),
     fill(circle(28, 17, 3.5), WHITE), fill(rect(25, 24, 6, 16, 3), WHITE))

# Roborock: the vacuum from above.
icon("com.roborock.smart", "roborock", None,
     fill(circle(28, 30, 22), "#FF4B1F"),
     fill(rect(21, 4, 14, 6, 3), "#FF4B1F"),
     fill(circle(28, 30, 7), WHITE))

# Google Home: the house outline in the four colours.
icon("com.google.android.apps.chromecast.app", "google_home", None,
     stroke("M28 7L8 25", BLUE, 6), stroke("M28 7l20 18", RED, 6),
     stroke("M45 23v26", YELLOW, 6), stroke("M11 23v26h34", GREEN, 6))

# -- the right page

# Amazon: the bag, with the smile.
icon("com.amazon.mShop.android.shopping", "amazon", None,
     fill("M12 18h32l3 30a4 4 0 0 1-4 4H13a4 4 0 0 1-4-4z", "#232F3E"),
     stroke("M20 8a8 8 0 0 1 16 0v10", "#232F3E"),
     stroke("M17 37c6 6 16 6 22 0", "#FF9900"), stroke("M36 33l3 4-5 1", "#FF9900"))

# Maps: the pin.
icon("com.google.android.apps.maps", "maps", None,
     fill("M28 52S9 31 9 21a19 19 0 0 1 38 0c0 10-19 31-19 31z", RED),
     fill(circle(28, 21, 7), WHITE))

# Calculator: the calculator.
icon("com.google.android.calculator", "calculator", None,
     fill(rect(12, 4, 32, 48, R), "#5F6368"),
     fill(rect(17, 9, 22, 10, 3), WHITE),
     fill(circle(21, 27, 3), WHITE), fill(circle(28, 27, 3), WHITE), fill(circle(35, 27, 3), WHITE),
     fill(circle(21, 36, 3), WHITE), fill(circle(28, 36, 3), WHITE), fill(circle(35, 36, 3), WHITE),
     fill(circle(21, 45, 3), WHITE), fill(circle(28, 45, 3), WHITE), fill(circle(35, 45, 3), BLUE))

# Pantry, the user's own: the jar.
icon("com.scenicprints.pantry", "pantry", None,
     fill(rect(19, 4, 18, 7, 3), "#4ADE80"),
     fill(rect(12, 12, 32, 40, R), "#4ADE80"),
     fill(rect(18, 24, 20, 14, 3), "#0E0F12"))

# YouTube: the screen.
icon("com.google.android.youtube", "youtube", None,
     fill(rect(4, 11, 48, 34, 10), "#E53935"),
     fill("M23 19l14 9-14 9z", WHITE))

# Instagram: the camera.
icon("com.instagram.android", "instagram", None,
     stroke(rect(8, 8, 40, 40, 12), "#E1306C"),
     stroke(circle(28, 28, 9), "#E1306C"),
     fill(circle(39, 17, 3), "#E1306C"))

# Reddit: the head.
icon("com.reddit.frontpage", "reddit", None,
     fill(circle(28, 32, 18), "#FF4500"),
     fill(circle(9, 29, 5), "#FF4500"), fill(circle(47, 29, 5), "#FF4500"),
     stroke("M28 14l6-9", "#FF4500", 3), fill(circle(36, 4.5, 3.5), "#FF4500"),
     fill(circle(21, 31, 3.5), WHITE), fill(circle(35, 31, 3.5), WHITE),
     stroke("M22 40c3 3 9 3 12 0", WHITE, 3))

# WhatsApp: the bubble, the handset.
icon("com.whatsapp", "whatsapp", None,
     fill("M28 5a23 23 0 1 1-12 42.5L6 50l2.7-9.8A23 23 0 0 1 28 5z", "#25D366"),
     fill("M21 16h4l2 6-3 2a12 12 0 0 0 7 7l2-3 6 2v4a3 3 0 0 1-3 3A19 19 0 0 1 18 19a3 3 0 0 1 3-3z", WHITE))

# Snapchat: the ghost.
icon("com.snapchat.android", "snapchat", None,
     fill("M28 5c10 0 15 7 15 16v6l6 3-6 3 3 6-7 2-2 5-9-3-9 3-2-5-7-2 3-6-6-3 6-3v-6c0-9 5-16 15-16z", "#FFFC00"))

# Poppy: a poppy. The app is not known here; the mark is a guess to be corrected.
icon("com.poppy.poppy", "poppy", None,
     fill(circle(28, 15, 11), "#E8412C"), fill(circle(15, 28, 11), "#E8412C"),
     fill(circle(41, 28, 11), "#E8412C"), fill(circle(28, 41, 11), "#E8412C"),
     fill(circle(28, 28, 7), INK))

# FuelWise, the user's own: the drop, with its gauge.
icon("com.fuelwise.fuelwise", "fuelwise", None,
     fill("M28 4c-11 15-18 24-18 32a18 18 0 0 0 36 0c0-8-7-17-18-32z", "#0F766E"),
     stroke(arc(28, 38, 9, 180, 360), "#134E4A", 4),
     stroke(arc(28, 38, 9, 180, 250), "#2DD4BF", 4),
     stroke("M28 38l5-6", WHITE, 3), fill(circle(28, 38, 2.5), WHITE))

# -- the Money deck

# Affirm: the disc, the a.
icon("com.affirm.central", "affirm", None,
     fill(circle(28, 28, 24), "#4A4AF4"),
     stroke(circle(27, 30, 8), WHITE), stroke("M35 22v16", WHITE))

# Capital One: the swoosh on navy.
icon("com.konylabs.capitalone", "capital_one", None,
     fill(circle(28, 28, 24), "#004977"),
     stroke("M13 34c8 2 22 0 30-9", "#D03027", 5))

# Citi: the arc over the dot.
icon("com.citi.citimobile", "citi", None,
     fill(circle(28, 28, 24), "#0066CC"),
     stroke(arc(28, 31, 13, 200, 340), "#EE1C25", 4), fill(circle(28, 32, 4.5), WHITE))

# U.S. Bank: the shield.
icon("com.usbank.mobilebanking", "us_bank", None,
     fill("M8 6h40v22c0 12-10 19-20 24C18 47 8 40 8 28z", "#0C2074"),
     fill("M8 6h40v14H8z", "#DA291C"))

# Venmo: the V.
icon("com.venmo", "venmo", None,
     stroke("M10 10l18 36 18-36", "#008CFF", 9))

# Wealthfront: the disc, the W.
icon("com.wealthfront", "wealthfront", None,
     fill(circle(28, 28, 24), "#4840BB"),
     stroke("M15 18l5 20 8-14 8 14 5-20", WHITE))

# Chase: the octagon.
icon("com.chase.sig.android", "chase", None,
     stroke("M20 6h16l14 14v16L36 50H20L6 36V20z", "#117ACA", 9))

# Discover: the card, the orange disc.
icon("com.discoverfinancial.mobile", "discover", None,
     fill(rect(4, 14, 48, 28, 8), WHITE),
     fill(circle(39, 28, 9), "#FF6000"), fill(rect(11, 24, 16, 8, 4), INK))

# PayPal: the two Ps.
icon("com.paypal.android.p2pmobile", "paypal", None,
     stroke("M16 46l6-36h11a9 9 0 0 1 0 18H22", "#003087", 7),
     stroke("M24 50l6-30h9a9 9 0 0 1 0 18h-9", "#009CDE", 7))

# Robinhood: the feather.
icon("com.robinhood.android", "robinhood", None,
     fill("M12 50C12 30 24 14 44 6c-4 16-14 32-32 44z", "#00C805"),
     stroke("M12 50l16-24", WHITE, 3))

# Synchrony: navy, the gold dot.
icon("com.syf.mysynchrony", "synchrony", None,
     fill(circle(28, 28, 24), "#003B5C"),
     stroke("M17 35c0-12 22-12 22 0", WHITE), fill(circle(28, 19, 4.5), "#FBC600"))

# -- the Bambu deck

# Bambu Handy: the hexagon.
icon("bbl.intl.bambulab.com", "bambu", None,
     fill("M28 4l21 12v24L28 52 7 40V16z", "#00AE42"),
     stroke("M28 16l10 6v12l-10 6-10-6V22z", WHITE))

# CyberBrick: the brick.
icon("com.bambulab.cyberbrick", "cyberbrick", None,
     fill(rect(4, 22, 48, 28, R), "#00AE42"),
     fill(rect(10, 12, 14, 10, 4), "#00AE42"), fill(rect(32, 12, 14, 10, 4), "#00AE42"),
     fill(rect(14, 32, 28, 8, 3), "#1B1B19"))

# -- the Travel deck

# Airbnb: the belo.
icon("com.airbnb.android", "airbnb", None,
     stroke("M28 6C21 18 11 33 11 40a7 7 0 0 0 13 3l4-7 4 7a7 7 0 0 0 13-3c0-7-10-22-17-34z", "#FF5A5F", 5),
     stroke(circle(28, 31, 4), "#FF5A5F", 4))

# Hostelworld: the disc, the H.
icon("com.hostelworld.app", "hostelworld", None,
     fill(circle(28, 28, 24), "#F7781F"),
     stroke("M19 16v24M37 16v24M19 28h18", WHITE))

# Uber: the disc, the U.
icon("com.ubercab", "uber", None,
     fill(circle(28, 28, 24), INK),
     stroke("M19 16v13a9 9 0 0 0 18 0V16", WHITE))

# Waymo: the W.
icon("com.waymo.carapp", "waymo", None,
     stroke("M8 12l7 32 13-22 13 22 7-32", "#1E88E5", 7))

# -- the phone's basics, so the drawer is drawn too

icon("com.android.chrome", "chrome", None,
     fill(wedge(28, 28, 24, -150, -30), RED), fill(wedge(28, 28, 24, -30, 90), YELLOW),
     fill(wedge(28, 28, 24, 90, 210), GREEN),
     fill(circle(28, 28, 13), WHITE), fill(circle(28, 28, 9), BLUE))

# Calendar: the page.
icon("com.google.android.calendar", "calendar", None,
     fill(rect(6, 6, 44, 44, R), WHITE),
     fill("M6 12a6 6 0 0 1 6-6h32a6 6 0 0 1 6 6v8H6z", BLUE),
     stroke("M19 28h7a4 4 0 0 1 0 8h-3h3a4 4 0 0 1 0 8h-7", INK, 3),
     stroke("M33 28l4-3v19", INK, 3))

icon("com.google.android.deskclock", "clock", None,
     fill(circle(28, 28, 24), BLUE), fill(circle(28, 28, 18), WHITE),
     stroke("M28 15v13l8 5", INK, 3))

icon("com.android.settings", "settings", None,
     fill(gear(28, 28, 24, 18, 8), "#5F6368"), fill(circle(28, 28, 7), WHITE))

# Play Store: the triangle.
icon("com.android.vending", "play", None,
     fill("M8 5l22 23L8 51z", BLUE),
     fill("M8 5l32 18-10 5z", GREEN),
     fill("M8 51l32-18-10-5z", RED),
     fill("M40 23l10 5-10 5-10-5z", YELLOW))

# Files: the folder.
icon("com.google.android.apps.nbu.files", "files", None,
     fill("M4 14a5 5 0 0 1 5-5h14l6 6h18a5 5 0 0 1 5 5v22a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5z", BLUE),
     fill(rect(4, 21, 48, 4), "#2A5BB8"))

icon("com.google.android.apps.docs", "drive", None,
     fill("M20 5h16l16 28H36z", YELLOW),
     fill("M4 33h32l-8 14H12z", BLUE),
     fill("M20 5L4 33l8 14 16-28z", GREEN))

# Keep: the bulb.
icon("com.google.android.keep", "keep", None,
     fill(circle(28, 23, 17), YELLOW),
     fill("M17 36h22v6a4 4 0 0 1-4 4H21a4 4 0 0 1-4-4z", WHITE),
     fill(rect(21, 47, 14, 4, 2), WHITE))

# Contacts: the person.
icon("com.google.android.contacts", "contacts", None,
     fill(circle(28, 17, 12), BLUE),
     fill("M6 52a22 22 0 0 1 44 0z", BLUE))

# Wallet: the cards.
icon("com.google.android.apps.walletnfcrel", "wallet", None,
     fill(rect(8, 8, 40, 12, R), BLUE), fill(rect(8, 20, 40, 12, R), GREEN),
     fill(rect(8, 32, 40, 12, R), YELLOW), fill(rect(28, 32, 20, 12, R), RED))

# Weather: the sun and the cloud.
icon("com.google.android.apps.weather", "weather", None,
     fill(circle(36, 20, 13), YELLOW),
     fill("M14 50a9 9 0 0 1 1-18 12 12 0 0 1 23-3 9 9 0 0 1 6 21z", WHITE))

# Meet: the camera.
icon("com.google.android.apps.tachyon", "meet", None,
     fill(rect(4, 14, 34, 28, R), "#00832D"),
     fill("M38 22l14-8v28l-14-8z", "#00AC47"))

icon("com.spotify.music", "spotify", None,
     fill(circle(28, 28, 24), "#1DB954"),
     stroke("M16 21a26 26 0 0 1 25 2", INK, 3.5), stroke("M18 28a20 20 0 0 1 19 2", INK, 3.5),
     stroke("M20 35a14 14 0 0 1 13 1.5", INK, 3.5))
